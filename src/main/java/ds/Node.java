package ds;

/**
 * Nodo genérico para estructuras de datos enlazadas.
 * Contiene un valor y referencia al siguiente nodo.
 * 
 * @param <T> Tipo de dato almacenado en el nodo
 */
public class Node<T> {
    T value;
    Node<T> next;
    
    public Node(T value) {
        this.value = value;
        this.next = null;
    }
    
    public T getValue() {
        return value;
    }
    
        public void setValue(T value) {
        this.value = value;
    }
    
    public Node<T> getNext() {
        return next;
    }
    
    public void setNext(Node<T> next) {
        this.next = next;
    }
}
