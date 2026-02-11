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
    private Policy currentPolicy;
    private int quantum;
    private int quantumCounter; // contador para RR
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
        this.currentPolicy = Policy.FCFS;
        this.quantum = 3;
        this.quantumCounter = 0;
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
        // Recorrer procesos bloqueados en RAM y serviciar su E/S
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
        
        // También serviciar E/S de procesos suspendidos bloqueados
        SinglyLinkedList<core.Process> stillSuspendedBlocked = new SinglyLinkedList<>();
        
        while (!suspendedBlocked.isEmpty()) {
            core.Process p = suspendedBlocked.removeFirst();
            
            if (p.getIoSpec() != null && p.getIoSpec().isGenerated()) {
                boolean satisfied = p.getIoSpec().serviceIO();
                
                if (satisfied) {
                    // E/S completada, mover a SUSPENDED_READY
                    p.setState(ProcessState.SUSPENDED_READY);
                    suspendedReady.addLast(p);
                    log.log(clock.getCurrentTick(), "Proceso " + p.getPid() + " desbloqueado en swap (E/S completada)");
                } else {
                    // Sigue bloqueado
                    stillSuspendedBlocked.addLast(p);
                }
            } else {
                stillSuspendedBlocked.addLast(p);
            }
        }
        
        suspendedBlocked = stillSuspendedBlocked;
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
        
        // SWAP OUT: Si excedemos RAM, suspender procesos de menor prioridad
        // Primero intentar suspender procesos READY
        while (processesInRam > ramLimit && !readyQueue.isEmpty()) {
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
        
        // Si aún excedemos RAM y no hay más READY, suspender BLOCKED
        while (processesInRam > ramLimit && !blockedQueue.isEmpty()) {
            core.Process toSwap = findLowestPriority(blockedQueue);
            if (toSwap != null) {
                blockedQueue.remove(toSwap);
                toSwap.setState(ProcessState.SUSPENDED_BLOCKED);
                suspendedBlocked.addLast(toSwap);
                log.log(clock.getCurrentTick(), "Proceso bloqueado " + toSwap.getPid() + " suspendido (RAM limit)");
                processesInRam--;
            } else {
                break;
            }
        }
        
        // SWAP IN: Si hay espacio en RAM, traer procesos suspendidos de vuelta
        // Priorizar procesos con mayor prioridad o deadlines cercanos
        while (processesInRam < ramLimit && !suspendedReady.isEmpty()) {
            // Buscar el proceso de mayor prioridad en suspendidos
            core.Process toRestore = findHighestPriorityInSuspended();
            if (toRestore != null) {
                suspendedReady.remove(toRestore);
                toRestore.setState(ProcessState.READY);
                readyQueue.addLast(toRestore);
                log.log(clock.getCurrentTick(), "Proceso " + toRestore.getPid() + " restaurado a RAM");
                processesInRam++;
            } else {
                break;
            }
        }
        
        // También manejar procesos bloqueados que se suspendieron
        // Si se desbloquearon mientras estaban suspendidos, moverlos a SUSPENDED_READY
        SinglyLinkedList<core.Process> stillSuspendedBlocked = new SinglyLinkedList<>();
        while (!suspendedBlocked.isEmpty()) {
            core.Process p = suspendedBlocked.removeFirst();
            
            // Si su E/S se completó, mover a SUSPENDED_READY
            if (p.getIoSpec() != null && p.getIoSpec().isSatisfied()) {
                p.setState(ProcessState.SUSPENDED_READY);
                suspendedReady.addLast(p);
                log.log(clock.getCurrentTick(), "Proceso " + p.getPid() + " movido a SUSPENDED_READY (E/S completada)");
            } else {
                stillSuspendedBlocked.addLast(p);
            }
        }
        suspendedBlocked = stillSuspendedBlocked;
    }
    
    // 5. Planificación y preemption
    private void scheduleOrPreempt() {
        // Verificar preemption según política
        if (running != null && !readyQueue.isEmpty()) {
            boolean shouldPreempt = false;
            
            // RR: verificar quantum
            if (currentPolicy == Policy.RR) {
                if (quantumCounter >= quantum) {
                    shouldPreempt = true;
                    log.log(clock.getCurrentTick(), "Quantum agotado para proceso " + running.getPid());
                }
            }
            // SRT: preempta si llega proceso con menos tiempo restante
            else if (currentPolicy == Policy.SRT) {
                core.Process shortest = findShortestRemaining(readyQueue);
                if (shortest != null && shortest.getInstructionsRemaining() < running.getInstructionsRemaining()) {
                    shouldPreempt = true;
                    log.log(clock.getCurrentTick(), "Preemption SRT: proceso más corto disponible");
                }
            }
            // PRIO: preempta si llega proceso de mayor prioridad
            else if (currentPolicy == Policy.PRIO) {
                core.Process highest = findHighestPriority(readyQueue);
                if (highest != null && highest.getPriority() > running.getPriority()) {
                    shouldPreempt = true;
                    log.log(clock.getCurrentTick(), "Preemption PRIO: proceso de mayor prioridad disponible");
                }
            }
            // EDF: preempta si llega proceso con deadline más cercano
            else if (currentPolicy == Policy.EDF) {
                core.Process earliest = findEarliestDeadline(readyQueue);
                if (earliest != null && earliest.getDeadlineRemaining(clock.getCurrentTick()) < running.getDeadlineRemaining(clock.getCurrentTick())) {
                    shouldPreempt = true;
                    log.log(clock.getCurrentTick(), "Preemption EDF: proceso con deadline más cercano disponible");
                }
            }
            
            // Aplicar preemption si es necesario
            if (shouldPreempt) {
                running.setState(ProcessState.READY);
                readyQueue.addLast(running);
                running = null;
                quantumCounter = 0;
            }
        }
        
        // Si no hay proceso corriendo, seleccionar uno
        if (running == null && !readyQueue.isEmpty()) {
            running = selectNextProcess();
            if (running != null) {
                running.setState(ProcessState.RUNNING);
                quantumCounter = 0;
                log.log(clock.getCurrentTick(), "Proceso " + running.getPid() + " seleccionado para ejecución");
            }
        }
        
        // Incrementar contador de quantum si hay proceso corriendo
        if (running != null && currentPolicy == Policy.RR) {
            quantumCounter++;
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
        snapshot.currentPolicy = currentPolicy.toString();
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
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        switch (currentPolicy) {
            case FCFS:
                // First-Come-First-Served: el primero en llegar
                return readyQueue.removeFirst();
                
            case RR:
                // Round Robin: igual que FCFS pero con quantum
                return readyQueue.removeFirst();
                
            case SRT:
                // Shortest Remaining Time: el de menor tiempo restante
                return removeShortestRemaining(readyQueue);
                
            case PRIO:
                // Prioridad Estática: el de mayor prioridad
                return removeHighestPriority(readyQueue);
                
            case EDF:
                // Earliest Deadline First: el de deadline más cercano
                return removeEarliestDeadline(readyQueue);
                
            default:
                return readyQueue.removeFirst();
        }
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
    
    // Helper para swap in - busca el de mayor prioridad en suspendidos
    private core.Process findHighestPriorityInSuspended() {
        Object[] array = suspendedReady.toArray();
        if (array.length == 0) return null;
        
        core.Process highest = (core.Process) array[0];
        for (Object obj : array) {
            core.Process p = (core.Process) obj;
            if (p.getPriority() > highest.getPriority()) {
                highest = p;
            }
        }
        return highest;
    }
    
    // Helpers para SRT
    private core.Process findShortestRemaining(SinglyLinkedList<core.Process> list) {
        Object[] array = list.toArray();
        if (array.length == 0) return null;
        
        core.Process shortest = (core.Process) array[0];
        for (Object obj : array) {
            core.Process p = (core.Process) obj;
            if (p.getInstructionsRemaining() < shortest.getInstructionsRemaining()) {
                shortest = p;
            }
        }
        return shortest;
    }
    
    private core.Process removeShortestRemaining(SinglyLinkedList<core.Process> list) {
        core.Process shortest = findShortestRemaining(list);
        if (shortest != null) {
            list.remove(shortest);
        }
        return shortest;
    }
    
    // Helpers para Prioridad
    private core.Process findHighestPriority(SinglyLinkedList<core.Process> list) {
        Object[] array = list.toArray();
        if (array.length == 0) return null;
        
        core.Process highest = (core.Process) array[0];
        for (Object obj : array) {
            core.Process p = (core.Process) obj;
            if (p.getPriority() > highest.getPriority()) {
                highest = p;
            }
        }
        return highest;
    }
    
    private core.Process removeHighestPriority(SinglyLinkedList<core.Process> list) {
        core.Process highest = findHighestPriority(list);
        if (highest != null) {
            list.remove(highest);
        }
        return highest;
    }
    
    // Helpers para EDF
    private core.Process findEarliestDeadline(SinglyLinkedList<core.Process> list) {
        Object[] array = list.toArray();
        if (array.length == 0) return null;
        
        core.Process earliest = (core.Process) array[0];
        int currentTick = clock.getCurrentTick();
        
        for (Object obj : array) {
            core.Process p = (core.Process) obj;
            if (p.getDeadlineRemaining(currentTick) < earliest.getDeadlineRemaining(currentTick)) {
                earliest = p;
            }
        }
        return earliest;
    }
    
    private core.Process removeEarliestDeadline(SinglyLinkedList<core.Process> list) {
        core.Process earliest = findEarliestDeadline(list);
        if (earliest != null) {
            list.remove(earliest);
        }
        return earliest;
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
    
    public void setPolicy(Policy policy) {
        this.currentPolicy = policy;
        this.quantumCounter = 0; // reiniciar contador al cambiar política
        log.log(clock.getCurrentTick(), "Política cambiada a: " + policy);
    }
    
    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }
    
    public Clock getClock() {
        return clock;
    }
}
