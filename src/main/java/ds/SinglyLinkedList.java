package ds;

/**
 * Lista enlazada simple implementada sin usar Collections de Java.
 * Soporta operaciones básicas y conversión a arreglo para snapshots.
 * 
 * @param <T> Tipo de dato almacenado en la lista
 */
public class SinglyLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;
    
    public SinglyLinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }
    
    /**
     * Agrega un elemento al final de la lista.
     * Complejidad: O(1)
     */
    public void addLast(T value) {
        Node<T> newNode = new Node<>(value);
        
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.setNext(newNode);
            tail = newNode;
        }
        size++;
    }
    
    /**
     * Agrega un elemento al inicio de la lista.
     * Complejidad: O(1)
     */
    public void addFirst(T value) {
        Node<T> newNode = new Node<>(value);
        
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.setNext(head);
            head = newNode;
        }
        size++;
    }
    
    /**
     * Remueve y retorna el primer elemento.
     * Complejidad: O(1)
     * 
     * @return El primer elemento, o null si la lista está vacía
     */
    public T removeFirst() {
        if (head == null) {
            return null;
        }
        
        T value = head.getValue();
        head = head.getNext();
        size--;
        
        if (head == null) {
            tail = null;
        }
        
        return value;
    }
    
    /**
     * Remueve la primera ocurrencia del elemento especificado.
     * Complejidad: O(n)
     * 
     * @return true si se removió, false si no se encontró
     */
    public boolean remove(T value) {
        if (head == null) {
            return false;
        }
        
        // Caso especial: remover el head
        if (head.getValue().equals(value)) {
            head = head.getNext();
            size--;
            if (head == null) {
                tail = null;
            }
            return true;
        }
        
        // Buscar en el resto de la lista
        Node<T> current = head;
        while (current.getNext() != null) {
            if (current.getNext().getValue().equals(value)) {
                Node<T> toRemove = current.getNext();
                current.setNext(toRemove.getNext());
                
                if (toRemove == tail) {
                    tail = current;
                }
                
                size--;
                return true;
            }
            current = current.getNext();
        }
        
        return false;
    }
    
    /**
     * Obtiene el primer elemento sin removerlo.
     * 
     * @return El primer elemento, o null si la lista está vacía
     */
    public T getFirst() {
        return head == null ? null : head.getValue();
    }
    
    /**
     * Obtiene el último elemento sin removerlo.
     * 
     * @return El último elemento, o null si la lista está vacía
     */
    public T getLast() {
        return tail == null ? null : tail.getValue();
    }
    
    /**
     * Verifica si la lista está vacía.
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Retorna el tamaño de la lista.
     */
    public int size() {
        return size;
    }
    
    /**
     * Limpia toda la lista.
     */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }
    
    /**
     * Convierte la lista a un arreglo de Object.
     * CRÍTICO para snapshots sin usar Collections.
     * Complejidad: O(n)
     * 
     * Nota: Retorna Object[] porque Java no permite crear arreglos genéricos
     * de forma segura sin reflexión. El caller debe hacer cast si necesita.
     * 
     * @return Arreglo con los elementos de la lista
     */
    public Object[] toArray() {
        Object[] array = new Object[size];
        Node<T> current = head;
        int index = 0;
        
        while (current != null) {
            array[index++] = current.getValue();
            current = current.getNext();
        }
        
        return array;
    }
    
    /**
     * Inserta un elemento en posición ordenada según un comparador.
     * Útil para políticas de scheduling ordenadas (EDF, SRT, PRIO).
     * Complejidad: O(n)
     * 
     * @param value Valor a insertar
     * @param comparator Función que retorna true si el primer arg debe ir antes que el segundo
     */
    public void insertOrdered(T value, Comparator<T> comparator) {
        Node<T> newNode = new Node<>(value);
        
        // Lista vacía o insertar al inicio
        if (head == null || comparator.compare(value, head.getValue())) {
            newNode.setNext(head);
            head = newNode;
            if (tail == null) {
                tail = newNode;
            }
            size++;
            return;
        }
        
        // Buscar posición correcta
        Node<T> current = head;
        while (current.getNext() != null && !comparator.compare(value, current.getNext().getValue())) {
            current = current.getNext();
        }
        
        // Insertar después de current
        newNode.setNext(current.getNext());
        current.setNext(newNode);
        
        if (newNode.getNext() == null) {
            tail = newNode;
        }
        
        size++;
    }
    
    /**
     * Interfaz simple para comparación sin usar java.util.Comparator
     */
    public interface Comparator<T> {
        /**
         * @return true si a debe ir antes que b
         */
        boolean compare(T a, T b);
    }
    
    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        
        while (current != null) {
            sb.append(current.getValue());
            if (current.getNext() != null) {
                sb.append(", ");
            }
            current = current.getNext();
        }
        
        sb.append("]");
        return sb.toString();
    }
}
