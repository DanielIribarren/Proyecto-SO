package metrics;

// Recolector de métricas del simulador RTOS
// Calcula métricas de rendimiento y cumplimiento de deadlines
public class MetricsCollector {
    
    // Contadores básicos
    private int totalProcesses;
    private int completedProcesses;
    private int missedDeadlines;
    
    // Tiempos acumulados
    private int totalWaitTime;
    private int totalTurnaroundTime;
    private int totalCpuTime;
    private int totalSimulationTicks;
    
    // Métricas de CPU
    private int ticksWithCpu;
    private int ticksIdle;
    
    public MetricsCollector() {
        reset();
    }
    
    public void reset() {
        totalProcesses = 0;
        completedProcesses = 0;
        missedDeadlines = 0;
        totalWaitTime = 0;
        totalTurnaroundTime = 0;
        totalCpuTime = 0;
        totalSimulationTicks = 0;
        ticksWithCpu = 0;
        ticksIdle = 0;
    }
    
    // Registrar un proceso completado
    public void recordCompletedProcess(int waitTime, int turnaroundTime, int cpuTime, boolean missedDeadline) {
        completedProcesses++;
        totalWaitTime += waitTime;
        totalTurnaroundTime += turnaroundTime;
        totalCpuTime += cpuTime;
        
        if (missedDeadline) {
            missedDeadlines++;
        }
    }
    
    // Registrar un tick de simulación
    public void recordTick(boolean cpuBusy) {
        totalSimulationTicks++;
        if (cpuBusy) {
            ticksWithCpu++;
        } else {
            ticksIdle++;
        }
    }
    
    // Registrar total de procesos creados
    public void setTotalProcesses(int total) {
        this.totalProcesses = total;
    }
    
    // === MÉTRICAS CALCULADAS ===
    
    // Tasa de éxito de misión (% de procesos que cumplieron deadline)
    public double getSuccessRate() {
        if (completedProcesses == 0) return 0.0;
        int successful = completedProcesses - missedDeadlines;
        return (successful * 100.0) / completedProcesses;
    }
    
    // Throughput (procesos completados por tick)
    public double getThroughput() {
        if (totalSimulationTicks == 0) return 0.0;
        return (double) completedProcesses / totalSimulationTicks;
    }
    
    // Utilización de CPU (% de tiempo que CPU estuvo ocupada)
    public double getCpuUtilization() {
        if (totalSimulationTicks == 0) return 0.0;
        return (ticksWithCpu * 100.0) / totalSimulationTicks;
    }
    
    // Tiempo de espera promedio
    public double getAverageWaitTime() {
        if (completedProcesses == 0) return 0.0;
        return (double) totalWaitTime / completedProcesses;
    }
    
    // Tiempo de turnaround promedio
    public double getAverageTurnaroundTime() {
        if (completedProcesses == 0) return 0.0;
        return (double) totalTurnaroundTime / completedProcesses;
    }
    
    // Tiempo de CPU promedio
    public double getAverageCpuTime() {
        if (completedProcesses == 0) return 0.0;
        return (double) totalCpuTime / completedProcesses;
    }
    
    // === GETTERS ===
    
    public int getTotalProcesses() {
        return totalProcesses;
    }
    
    public int getCompletedProcesses() {
        return completedProcesses;
    }
    
    public int getMissedDeadlines() {
        return missedDeadlines;
    }
    
    public int getTotalSimulationTicks() {
        return totalSimulationTicks;
    }
    
    public int getTicksWithCpu() {
        return ticksWithCpu;
    }
    
    public int getTicksIdle() {
        return ticksIdle;
    }
    
    @Override
    public String toString() {
        return String.format(
            "Metrics[completed=%d/%d, success=%.1f%%, throughput=%.3f, cpuUtil=%.1f%%, avgWait=%.1f, avgTurnaround=%.1f]",
            completedProcesses, totalProcesses, getSuccessRate(), getThroughput(),
            getCpuUtilization(), getAverageWaitTime(), getAverageTurnaroundTime()
        );
    }
}
