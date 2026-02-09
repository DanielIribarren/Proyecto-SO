package core;

/**
 * Representa un evento de interrupción en el sistema RTOS.
 * Las interrupciones son eventos asíncronos que requieren atención inmediata.
 */
public class InterruptEvent {
    private String type;
    private int priority;
    private int tickGenerated;
    private String description;
    
    /**
     * @param type Tipo de interrupción (ej: "MICRO_METEORITE", "SOLAR_FLARE", "EMERGENCY")
     * @param priority Prioridad de la interrupción (mayor = más urgente)
     * @param tickGenerated Tick del reloj en que se generó
     * @param description Descripción del evento
     */
    public InterruptEvent(String type, int priority, int tickGenerated, String description) {
        this.type = type;
        this.priority = priority;
        this.tickGenerated = tickGenerated;
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public int getTickGenerated() {
        return tickGenerated;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return "Interrupt[" + type + ", priority=" + priority + ", tick=" + tickGenerated + "]";
    }
}
