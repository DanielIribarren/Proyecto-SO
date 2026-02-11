package sim;

import core.InterruptEvent;

// Thread que genera interrupciones asíncronas para simular eventos externos
// Representa sensores, comunicaciones, errores, etc. del microsatélite
public class InterruptGenerator extends Thread {
    
    private SimKernel kernel;
    private boolean running;
    private int minInterval;
    private int maxInterval;
    
    // Tipos de interrupciones que puede generar
    private static final String[] INTERRUPT_TYPES = {
        "SENSOR_DATA",      // Datos de sensores
        "COMM_SIGNAL",      // Señal de comunicación
        "TIMER_TICK",       // Timer del sistema
        "ERROR_DETECTED",   // Error detectado
        "BATTERY_LOW"       // Batería baja
    };
    
    // Prioridades de interrupciones (mayor = más urgente)
    private static final int[] INTERRUPT_PRIORITIES = {
        5,  // SENSOR_DATA
        7,  // COMM_SIGNAL
        3,  // TIMER_TICK
        9,  // ERROR_DETECTED
        8   // BATTERY_LOW
    };
    
    public InterruptGenerator(SimKernel kernel, int minInterval, int maxInterval) {
        this.kernel = kernel;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.running = false;
        this.setDaemon(true); // Thread daemon para que no impida cerrar el programa
        this.setName("InterruptGenerator");
    }
    
    @Override
    public void run() {
        running = true;
        
        while (running) {
            try {
                // Esperar un intervalo aleatorio entre interrupciones
                int interval = minInterval + (int)(Math.random() * (maxInterval - minInterval + 1));
                Thread.sleep(interval);
                
                if (!running) break;
                
                // Generar una interrupción aleatoria
                int typeIndex = (int)(Math.random() * INTERRUPT_TYPES.length);
                String type = INTERRUPT_TYPES[typeIndex];
                int priority = INTERRUPT_PRIORITIES[typeIndex];
                
                // Obtener tick actual del kernel
                int currentTick = kernel.getClock().getCurrentTick();
                String description = "Evento externo: " + type;
                
                InterruptEvent event = new InterruptEvent(type, priority, currentTick, description);
                kernel.addInterrupt(event);
                
            } catch (InterruptedException e) {
                // Thread interrumpido, salir
                break;
            }
        }
    }
    
    // Detener el generador de interrupciones
    public void stopGenerator() {
        running = false;
        this.interrupt();
    }
    
    public boolean isRunning() {
        return running;
    }
}
