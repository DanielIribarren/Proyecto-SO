package sim;

import core.InterruptEvent;
import core.ProcessState;
import ds.Queue;
import ds.SinglyLinkedList;
import java.util.concurrent.Semaphore;

// Núcleo del simulador RTOS
// Maneja todas las colas de procesos y ejecuta el ciclo de simulación
public class SimKernel {
    // Sincronización
    private final Semaphore mutex = new Semaphore(1);
    
    // Reloj y log
    private Clock clock;
    private EventLog log;
    
    // Colas de procesos
    private SinglyLinkedList<core.Process> newQueue;
    private SinglyLinkedList<core.Process> readyQueue;
    private SinglyLinkedList<core.Process> blockedQueue;
    private SinglyLinkedList<core.Process> suspendedReady;
    private SinglyLinkedList<core.Process> suspendedBlocked;
    private SinglyLinkedList<core.Process> terminated;
    
    // Proceso en ejecución
    private core.Process running;
    
    // Cola de interrupciones
    private Queue<InterruptEvent> interruptQueue;
    
    // Configuración
    private String currentPolicy;
    private int quantum;
    private int ramLimit;
    
    // Snapshot actual
    private SystemSnapshot currentSnapshot;
    
    public SimKernel() {
        this.clock = new Clock();
        this.log = new EventLog();
        
        // Inicializar colas
        this.newQueue = new SinglyLinkedList<>();
        this.readyQueue = new SinglyLinkedList<>();
        this.blockedQueue = new SinglyLinkedList<>();
        this.suspendedReady = new SinglyLinkedList<>();
        this.suspendedBlocked = new SinglyLinkedList<>();
        this.terminated = new SinglyLinkedList<>();
        this.interruptQueue = new Queue<>();
        
        this.running = null;
        this.currentPolicy = "FCFS";
        this.quantum = 3;
        this.ramLimit = 10;
        
        this.currentSnapshot = new SystemSnapshot();
    }
    
    // TICK CONTRACT - 8 pasos que NUNCA cambian
    public void tick() {
        try {
            mutex.acquire();
            
            // 1. Procesar interrupciones pendientes
            handleInterrupts();
            
            // 2. Actualizar E/S (servicios y desbloqueos)
            updateIO();
            
            // 3. Admitir procesos (NEW → READY)
            admitNewProcesses();
            
            // 4. Aplicar swap si RAM_LIMIT excedido
            applyMediumTermSwap();
            
            // 5. Planificar (short-term) + preemption
            scheduleOrPreempt();
            
            // 6. Ejecutar 1 instrucción del RUNNING
            executeOneInstruction();
            
            // 7. Actualizar métricas
            updateMetrics();
            
            // 8. Generar snapshot + avanzar reloj
            currentSnapshot = buildSnapshot();
            clock.tick();
            
        } catch (InterruptedException e) {
            log.log(clock.getCurrentTick(), "ERROR: Interrupción en mutex");
        } finally {
            mutex.release();
        }
    }
    
    // 1. Procesar interrupciones
    private void handleInterrupts() {
        if (interruptQueue.isEmpty()) {
            return;
        }
        
        InterruptEvent event = interruptQueue.dequeue();
        log.log(clock.getCurrentTick(), "Interrupción: " + event.getType());
        
        // Si hay proceso corriendo, moverlo a READY
        if (running != null) {
            running.setState(ProcessState.READY);
            readyQueue.addFirst(running);
            log.log(clock.getCurrentTick(), "Proceso " + running.getPid() + " preemptado por interrupción");
        }
        
        // Crear ISR y ponerlo a correr
        core.Process isr = core.Process.createISR(9999, "ISR_" + event.getType(), 5, clock.getCurrentTick());
        isr.setState(ProcessState.RUNNING);
        running = isr;
        log.log(clock.getCurrentTick(), "ISR iniciada: " + isr.getName());
    }
    
    // 2. Actualizar E/S
    private void updateIO() {
        // Recorrer procesos bloqueados y serviciar su E/S
        SinglyLinkedList<core.Process> stillBlocked = new SinglyLinkedList<>();
        
        while (!blockedQueue.isEmpty()) {
            core.Process p = blockedQueue.removeFirst();
            
            if (p.getIoSpec() != null && p.getIoSpec().isGenerated()) {
                boolean satisfied = p.getIoSpec().serviceIO();
                
                if (satisfied) {
                    // E/S completada, mover a READY
                    p.setState(ProcessState.READY);
                    readyQueue.addLast(p);
                    log.log(clock.getCurrentTick(), "Proceso " + p.getPid() + " desbloqueado (E/S completada)");
                } else {
                    // Sigue bloqueado
                    stillBlocked.addLast(p);
                }
            } else {
                stillBlocked.addLast(p);
            }
        }
        
        // Restaurar cola de bloqueados
        blockedQueue = stillBlocked;
    }
    
    // 3. Admitir nuevos procesos
    private void admitNewProcesses() {
        int processesInRam = readyQueue.size() + blockedQueue.size() + (running != null ? 1 : 0);
        
        while (!newQueue.isEmpty() && processesInRam < ramLimit) {
            core.Process p = newQueue.removeFirst();
            
            // Verificar si ya llegó su arrival tick
            if (p.getArrivalTick() <= clock.getCurrentTick()) {
                p.setState(ProcessState.READY);
                readyQueue.addLast(p);
                log.log(clock.getCurrentTick(), "Proceso " + p.getPid() + " admitido a READY");
                processesInRam++;
            } else {
                // Todavía no llega, devolver a NEW
                newQueue.addFirst(p);
                break;
            }
        }
    }
    
    // 4. Swap (planificador de mediano plazo)
    private void applyMediumTermSwap() {
        int processesInRam = readyQueue.size() + blockedQueue.size() + (running != null ? 1 : 0);
        
        // Si excedemos RAM, suspender procesos de menor prioridad
        while (processesInRam > ramLimit && !readyQueue.isEmpty()) {
            // Buscar proceso de menor prioridad en READY
            core.Process toSwap = findLowestPriority(readyQueue);
            if (toSwap != null) {
                readyQueue.remove(toSwap);
                toSwap.setState(ProcessState.SUSPENDED_READY);
                suspendedReady.addLast(toSwap);
                log.log(clock.getCurrentTick(), "Proceso " + toSwap.getPid() + " suspendido (RAM limit)");
                processesInRam--;
            } else {
                break;
            }
        }
    }
    
    // 5. Planificación y preemption
    private void scheduleOrPreempt() {
        // Si no hay proceso corriendo, seleccionar uno
        if (running == null && !readyQueue.isEmpty()) {
            running = selectNextProcess();
            if (running != null) {
                running.setState(ProcessState.RUNNING);
                log.log(clock.getCurrentTick(), "Proceso " + running.getPid() + " seleccionado para ejecución");
            }
        }
    }
    
    // 6. Ejecutar instrucción
    private void executeOneInstruction() {
        if (running == null) {
            return;
        }
        
        boolean finished = running.executeInstruction();
        
        // Verificar si generó E/S
        if (running.getIoSpec() != null && running.getIoSpec().isGenerated() && !running.getIoSpec().isSatisfied()) {
            // Bloquear por E/S
            running.setState(ProcessState.BLOCKED);
            blockedQueue.addLast(running);
            log.log(clock.getCurrentTick(), "Proceso " + running.getPid() + " bloqueado por E/S");
            running = null;
            return;
        }
        
        // Verificar si terminó
        if (finished) {
            running.terminate(clock.getCurrentTick());
            terminated.addLast(running);
            log.log(clock.getCurrentTick(), "Proceso " + running.getPid() + " terminado");
            running = null;
            return;
        }
        
        // Verificar deadline miss
        if (running.hasMissedDeadline(clock.getCurrentTick())) {
            running.setMissedDeadline(true);
            log.log(clock.getCurrentTick(), "DEADLINE MISS: Proceso " + running.getPid());
        }
    }
    // 7. Actualizar métricas
    private void updateMetrics() {
        // Incrementar wait time de procesos en READY
        Object[] readyArray = readyQueue.toArray();
        for (Object obj : readyArray) {
            core.Process p = (core.Process) obj;
            p.incrementWaitTime();
        }
    }

    // 8. Generar snapshot
    private SystemSnapshot buildSnapshot() {
        SystemSnapshot snapshot = new SystemSnapshot();
        snapshot.currentTick = clock.getCurrentTick();
        snapshot.currentPolicy = currentPolicy;
        snapshot.quantum = quantum;
        
        // Proceso corriendo
        snapshot.running = running != null ? createProcessInfo(running) : null;
        
        // Colas
        snapshot.readyQueue = convertToProcessInfoArray(readyQueue);
        snapshot.blockedQueue = convertToProcessInfoArray(blockedQueue);
        snapshot.suspendedReady = convertToProcessInfoArray(suspendedReady);
        snapshot.suspendedBlocked = convertToProcessInfoArray(suspendedBlocked);
        snapshot.terminated = convertToProcessInfoArray(terminated);
        
        // Logs
        snapshot.logs = log.toArray();
        
        // Métricas básicas
        snapshot.totalProcesses = newQueue.size() + readyQueue.size() + blockedQueue.size() + 
                                  suspendedReady.size() + suspendedBlocked.size() + 
                                  terminated.size() + (running != null ? 1 : 0);
        snapshot.completedProcesses = terminated.size();
        snapshot.cpuUtilization = running != null ? 100.0 : 0.0;
        
        return snapshot;
    }
    
    // Helpers
    
    private core.Process selectNextProcess() {
        // Por ahora FCFS simple
        return readyQueue.removeFirst();
    }
    
    private core.Process findLowestPriority(SinglyLinkedList<core.Process> list) {
        Object[] array = list.toArray();
        if (array.length == 0) return null;
        
        core.Process lowest = (core.Process) array[0];
        for (Object obj : array) {
            core.Process p = (core.Process) obj;
            if (p.getPriority() < lowest.getPriority()) {
                lowest = p;
            }
        }
        return lowest;
    }
    
    private SystemSnapshot.ProcessInfo createProcessInfo(core.Process p) {
        return new SystemSnapshot.ProcessInfo(
            p.getPid(),
            p.getName(),
            p.getState().toString(),
            p.getPc(),
            p.getMar(),
            p.getPriority(),
            p.getInstructionsRemaining(),
            p.getDeadlineRemaining(clock.getCurrentTick()),
            p.isMissedDeadline(),
            p.isISR()
        );
    }
    
    private SystemSnapshot.ProcessInfo[] convertToProcessInfoArray(SinglyLinkedList<core.Process> list) {
        Object[] array = list.toArray();
        SystemSnapshot.ProcessInfo[] result = new SystemSnapshot.ProcessInfo[array.length];
        
        for (int i = 0; i < array.length; i++) {
            result[i] = createProcessInfo((core.Process) array[i]);
        }
        
        return result;
    }
    
    // Métodos públicos para control
    
    public void addProcess(core.Process p) {
        try {
            mutex.acquire();
            newQueue.addLast(p);
            log.log(clock.getCurrentTick(), "Proceso " + p.getPid() + " agregado a NEW");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release();
        }
    }
    
    public void addInterrupt(InterruptEvent event) {
        try {
            mutex.acquire();
            interruptQueue.enqueue(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release();
        }
    }
    
    public SystemSnapshot getSnapshot() {
        return currentSnapshot;
    }
    
    public void setPolicy(String policy) {
        this.currentPolicy = policy;
    }
    
    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }
    
    public Clock getClock() {
        return clock;
    }
}
