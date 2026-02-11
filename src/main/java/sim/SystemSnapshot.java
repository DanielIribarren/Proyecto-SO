package sim;

// Snapshot del estado del sistema en un tick específico
// La UI consume esto para mostrar el estado sin acceder directamente
// a las estructuras internas del simulador
public class SystemSnapshot {
    // Info básica
    public int currentTick;
    public String currentPolicy;
    public int quantum; // para Round Robin
    
    // Proceso en ejecución
    public ProcessInfo running;
    
    // Colas
    public ProcessInfo[] readyQueue;
    public ProcessInfo[] blockedQueue;
    public ProcessInfo[] suspendedReady;
    public ProcessInfo[] suspendedBlocked;
    public ProcessInfo[] terminated;
    
    // Logs
    public String[] logs;
    
    // Métricas
    public double cpuUtilization;
    public int totalProcesses;
    public int completedProcesses;
    public int missedDeadlines;
    public double successRate;
    public double throughput;
    public double averageWaitTime;
    public double averageTurnaroundTime;
    
    public SystemSnapshot() {
        // Inicializar arreglos vacíos por defecto
        this.readyQueue = new ProcessInfo[0];
        this.blockedQueue = new ProcessInfo[0];
        this.suspendedReady = new ProcessInfo[0];
        this.suspendedBlocked = new ProcessInfo[0];
        this.terminated = new ProcessInfo[0];
        this.logs = new String[0];
    }
    
    // Clase interna para info de proceso (simplificada para UI)
    public static class ProcessInfo {
        public int pid;
        public String name;
        public String state;
        public int pc;
        public int mar;
        public int priority;
        public int instructionsRemaining;
        public int deadlineRemaining;
        public boolean missedDeadline;
        public boolean isISR;
        
        public ProcessInfo(int pid, String name, String state, int pc, int mar,
                          int priority, int instructionsRemaining, 
                          int deadlineRemaining, boolean missedDeadline, boolean isISR) {
            this.pid = pid;
            this.name = name;
            this.state = state;
            this.pc = pc;
            this.mar = mar;
            this.priority = priority;
            this.instructionsRemaining = instructionsRemaining;
            this.deadlineRemaining = deadlineRemaining;
            this.missedDeadline = missedDeadline;
            this.isISR = isISR;
        }
        
        @Override
        public String toString() {
            return String.format("P%d[%s, %s, PC=%d, rem=%d, dl=%d]", 
                    pid, name, state, pc, instructionsRemaining, deadlineRemaining);
        }
    }
}