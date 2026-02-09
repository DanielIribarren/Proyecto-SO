package core;

/**
 * Especificación de una operación de E/S para un proceso.
 * Define cuándo se genera la excepción y cuánto tarda en satisfacerse.
 */
public class IOSpec {
    private int cyclesUntilGeneration;
    private int cyclesToSatisfy;
    private int cyclesRemaining;
    private boolean generated;
    private boolean satisfied;
    
    /**
     * @param cyclesUntilGeneration Cuántos ciclos de ejecución hasta generar la excepción de E/S
     * @param cyclesToSatisfy Cuántos ciclos necesita para satisfacer la E/S una vez generada
     */
    public IOSpec(int cyclesUntilGeneration, int cyclesToSatisfy) {
        this.cyclesUntilGeneration = cyclesUntilGeneration;
        this.cyclesToSatisfy = cyclesToSatisfy;
        this.cyclesRemaining = cyclesToSatisfy;
        this.generated = false;
        this.satisfied = false;
    }
    
    /**
     * Decrementa el contador hasta generar la E/S.
     * @return true si se generó la excepción en este ciclo
     */
    public boolean decrementUntilGeneration() {
        if (generated || satisfied) {
            return false;
        }
        
        cyclesUntilGeneration--;
        if (cyclesUntilGeneration <= 0) {
            generated = true;
            return true;
        }
        return false;
    }
    
    /**
     * Procesa un ciclo de servicio de E/S.
     * @return true si la E/S se satisfizo en este ciclo
     */
    public boolean serviceIO() {
        if (!generated || satisfied) {
            return false;
        }
        
        cyclesRemaining--;
        if (cyclesRemaining <= 0) {
            satisfied = true;
            return true;
        }
        return false;
    }
    
    public boolean isGenerated() {
        return generated;
    }
    
    public boolean isSatisfied() {
        return satisfied;
    }
    
    public int getCyclesRemaining() {
        return cyclesRemaining;
    }
    
    public int getCyclesUntilGeneration() {
        return cyclesUntilGeneration;
    }
    
    @Override
    public String toString() {
        if (satisfied) {
            return "IO[satisfied]";
        } else if (generated) {
            return "IO[servicing, remaining=" + cyclesRemaining + "]";
        } else {
            return "IO[waiting, until=" + cyclesUntilGeneration + "]";
        }
    }
}
