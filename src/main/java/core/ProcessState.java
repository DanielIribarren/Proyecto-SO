package core;

/**
 * Estados posibles de un proceso en el sistema RTOS.
 * Basado en el modelo de 7 estados del enunciado.
 */
public enum ProcessState {
    /**
     * Proceso recién creado, aún no admitido al sistema
     */
    NEW,
    
    /**
     * Proceso en memoria principal, listo para ejecutar
     */
    READY,
    
    /**
     * Proceso actualmente ejecutándose en CPU
     */
    RUNNING,
    
    /**
     * Proceso bloqueado esperando E/S en memoria principal
     */
    BLOCKED,
    
    /**
     * Proceso listo pero movido a memoria secundaria (swap)
     */
    SUSPENDED_READY,
    
    /**
     * Proceso bloqueado y movido a memoria secundaria (swap)
     */
    SUSPENDED_BLOCKED,
    
    /**
     * Proceso terminado (completado o abortado)
     */
    TERMINATED
}
