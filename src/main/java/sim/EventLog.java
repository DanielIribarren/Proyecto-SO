package sim;

import ds.Queue;

// Log de eventos del sistema
// Guarda los últimos 200 eventos para mostrar en la UI
public class EventLog {
    private Queue<String> entries;
    private static final int MAX_ENTRIES = 200;
    private int count;
    
    public EventLog() {
        this.entries = new Queue<>();
        this.count = 0;
    }
    
    // Agrega un evento al log con el tick actual
    public void log(int tick, String message) {
        String entry = "[t=" + tick + "] " + message;
        entries.enqueue(entry);
        count++;
        
        // Si pasamos el límite, removemos el más viejo
        if (count > MAX_ENTRIES) {
            entries.dequeue();
            count--;
        }
    }
    
    // Retorna todos los logs como arreglo para la UI
    public String[] toArray() {
        Object[] objArray = entries.toArray();
        String[] result = new String[objArray.length];
        for (int i = 0; i < objArray.length; i++) {
            result[i] = (String) objArray[i];
        }
        return result;
    }
    
    // Limpia el log
    public void clear() {
        entries.clear();
        count = 0;
    }
    
    public int size() {
        return count;
    }
}
