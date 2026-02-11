package sim;

// Políticas de planificación soportadas por el simulador
public enum Policy {
    // Propósito general
    FCFS,   // First-Come-First-Served
    RR,     // Round Robin
    SRT,    // Shortest Remaining Time
    
    // Tiempo real
    PRIO,   // Prioridad Estática Preemptiva
    EDF     // Earliest Deadline First
}
