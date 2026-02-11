package core;

/**
 * Representa un proceso en el sistema RTOS con su PCB (Process Control Block).
 * Contiene toda la información necesaria para gestionar el proceso.
 */
public class SimProcess {
    // Identificación
    private int pid;
    private String name;
    
    // Estado y control
    private ProcessState state;
    private int pc;  // Program Counter
    private int mar; // Memory Address Register
    
    // Características del proceso
    private int totalInstructions;
    private int instructionsRemaining;
    private int priority;
    
    // Tiempo real (deadlines)
    private int arrivalTick;
    private int relativeDeadline;
    private int absoluteDeadline;
    private boolean missedDeadline;
    
    // E/S
    private IOSpec ioSpec;
    
    // Métricas
    private int waitTime;
    private int turnaroundTime;
    private int completionTick;
    
    // Flags
    private boolean isISR; // Es una rutina de servicio de interrupción
    
    /**
     * Constructor para proceso normal
     */
    public SimProcess(int pid, String name, int totalInstructions, int priority, 
                      int arrivalTick, int relativeDeadline) {
        this.pid = pid;
        this.name = name;
        this.totalInstructions = totalInstructions;
        this.instructionsRemaining = totalInstructions;
        this.priority = priority;
        this.arrivalTick = arrivalTick;
        this.relativeDeadline = relativeDeadline;
        this.absoluteDeadline = arrivalTick + relativeDeadline;
        
        this.state = ProcessState.NEW;
        this.pc = 0;
        this.mar = 0;
        this.missedDeadline = false;
        this.isISR = false;
        this.waitTime = 0;
        this.completionTick = -1;
    }
    
    /**
     * Constructor para ISR (Interrupt Service Routine)
     */
    public static SimProcess createISR(int pid, String name, int instructions, int currentTick) {
        SimProcess isr = new SimProcess(pid, name, instructions, 9999, currentTick, 10);
        isr.isISR = true;
        return isr;
    }
    
    /**
     * Ejecuta una instrucción del proceso.
     * Incrementa PC y MAR, decrementa instrucciones restantes.
     * 
     * @return true si el proceso terminó, false si aún tiene instrucciones
     */
    public boolean executeInstruction() {
        if (instructionsRemaining <= 0) {
            return true;
        }
        
        pc++;
        mar++;
        instructionsRemaining--;
        
        // Verificar si hay E/S pendiente
        if (ioSpec != null && !ioSpec.isSatisfied()) {
            ioSpec.decrementUntilGeneration();
        }
        
        return instructionsRemaining == 0;
    }
    
    /**
     * Verifica si el proceso necesita E/S en este momento.
     */
    public boolean needsIO() {
        return ioSpec != null && ioSpec.isGenerated() && !ioSpec.isSatisfied();
    }
    
    /**
     * Verifica si el proceso generó una excepción de E/S.
     */
    public boolean generatedIO() {
        return ioSpec != null && ioSpec.isGenerated();
    }
    
    /**
     * Calcula el deadline restante dado el tick actual.
     */
    public int getDeadlineRemaining(int currentTick) {
        return absoluteDeadline - currentTick;
    }
    
    /**
     * Verifica si el proceso perdió su deadline.
     */
    public boolean hasMissedDeadline(int currentTick) {
        return currentTick > absoluteDeadline && state != ProcessState.TERMINATED;
    }
    
    /**
     * Marca el proceso como terminado.
     */
    public void terminate(int currentTick) {
        this.state = ProcessState.TERMINATED;
        this.completionTick = currentTick;
        this.turnaroundTime = currentTick - arrivalTick;
    }
    
    /**
     * Incrementa el tiempo de espera (llamar cada tick que está en READY).
     */
    public void incrementWaitTime() {
        waitTime++;
    }
    
    // Getters y Setters
    
    public int getPid() {
        return pid;
    }
    
    public String getName() {
        return name;
    }
    
    public ProcessState getState() {
        return state;
    }
    
    public void setState(ProcessState state) {
        this.state = state;
    }
    
    public int getPc() {
        return pc;
    }
    
    public int getMar() {
        return mar;
    }
    
    public int getTotalInstructions() {
        return totalInstructions;
    }
    
    public int getInstructionsRemaining() {
        return instructionsRemaining;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getArrivalTick() {
        return arrivalTick;
    }
    
    public int getRelativeDeadline() {
        return relativeDeadline;
    }
    
    public int getAbsoluteDeadline() {
        return absoluteDeadline;
    }
    
    public boolean isMissedDeadline() {
        return missedDeadline;
    }
    
    public void setMissedDeadline(boolean missed) {
        this.missedDeadline = missed;
    }
    
    public IOSpec getIoSpec() {
        return ioSpec;
    }
    
    public void setIoSpec(IOSpec ioSpec) {
        this.ioSpec = ioSpec;
    }
    
    public int getWaitTime() {
        return waitTime;
    }
    
    public int getTurnaroundTime() {
        return turnaroundTime;
    }
    
    public int getCompletionTick() {
        return completionTick;
    }
    
    public boolean isISR() {
        return isISR;
    }
    
    @Override
    public String toString() {
        return String.format("P%d[%s, state=%s, PC=%d, remaining=%d, deadline=%d, priority=%d]",
                pid, name, state, pc, instructionsRemaining, absoluteDeadline, priority);
    }
    
    /**
     * Retorna una representación detallada del PCB.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PCB ===\n");
        sb.append("PID: ").append(pid).append("\n");
        sb.append("Name: ").append(name).append("\n");
        sb.append("State: ").append(state).append("\n");
        sb.append("PC: ").append(pc).append("\n");
        sb.append("MAR: ").append(mar).append("\n");
        sb.append("Priority: ").append(priority).append("\n");
        sb.append("Instructions: ").append(instructionsRemaining).append("/").append(totalInstructions).append("\n");
        sb.append("Deadline: ").append(absoluteDeadline).append(" (relative: ").append(relativeDeadline).append(")\n");
        sb.append("Arrival: ").append(arrivalTick).append("\n");
        sb.append("Wait Time: ").append(waitTime).append("\n");
        if (ioSpec != null) {
            sb.append("I/O: ").append(ioSpec).append("\n");
        }
        sb.append("Is ISR: ").append(isISR).append("\n");
        return sb.toString();
    }
}
