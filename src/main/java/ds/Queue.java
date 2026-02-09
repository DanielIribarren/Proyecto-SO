package ds;

/**
 * Cola (FIFO) implementada sin usar Collections de Java.
 * Wrapper sobre SinglyLinkedList para operaciones de cola.
 * 
 * @param <T> Tipo de dato almacenado en la cola
 */
public class Queue<T> {
    private SinglyLinkedList<T> list;
    
    public Queue() {
        this.list = new SinglyLinkedList<>();
    }
    
    /**
     * Agrega un elemento al final de la cola.
     * Complejidad: O(1)
     */
    public void enqueue(T value) {
        list.addLast(value);
    }
    
    /**
     * Remueve y retorna el elemento al frente de la cola.
     * Complejidad: O(1)
     * 
     * @return El elemento al frente, o null si la cola está vacía
     */
    public T dequeue() {
        return list.removeFirst();
    }
    
    /**
     * Obtiene el elemento al frente sin removerlo.
     * 
     * @return El elemento al frente, o null si la cola está vacía
     */
    public T peek() {
        return list.getFirst();
    }
    
    /**
     * Verifica si la cola está vacía.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    /**
     * Retorna el tamaño de la cola.
     */
    public int size() {
        return list.size();
    }
    
    /**
     * Limpia toda la cola.
     */
    public void clear() {
        list.clear();
    }
    
    /**
     * Convierte la cola a un arreglo de Object.
     * CRÍTICO para snapshots sin usar Collections.
     * 
     * @return Arreglo con los elementos de la cola
     */
    public Object[] toArray() {
        return list.toArray();
    }
    
    @Override
    public String toString() {
        return list.toString();
    }

}
    

