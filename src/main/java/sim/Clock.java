package sim;

// Reloj global del sistema
// Lleva la cuenta de los ticks (ciclos) del simulador
public class Clock {
    private int currentTick;
    
    public Clock() {
        this.currentTick = 0;
    }
    
    // Avanza el reloj un tick
    public void tick() {
        currentTick++;
    }
    
    // Reinicia el reloj a 0
    public void reset() {
        currentTick = 0;
    }
    
    public int getCurrentTick() {
        return currentTick;
    }
    
    @Override
    public String toString() {
        return "Clock[tick=" + currentTick + "]";
    }
}
