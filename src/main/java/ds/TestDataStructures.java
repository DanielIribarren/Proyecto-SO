package ds;

/**
 * Clase de prueba simple para verificar estructuras de datos propias.
 * NO usa JUnit para mantener simplicidad y compatibilidad con NetBeans básico.
 */
public class TestDataStructures {
    
    public static void main(String[] args) {
        System.out.println("=== Iniciando pruebas de estructuras de datos ===\n");
        
        testNode();
        testSinglyLinkedList();
        testQueue();
        testInsertOrdered();
        
        System.out.println("\n=== Todas las pruebas completadas ===");
    }
    
    private static void testNode() {
        System.out.println("--- Test Node ---");
        Node<Integer> node = new Node<>(42);
        assert node.getValue() == 42 : "Error: valor incorrecto";
        assert node.getNext() == null : "Error: next debería ser null";
        
        Node<Integer> node2 = new Node<>(100);
        node.setNext(node2);
        assert node.getNext() == node2 : "Error: next no se asignó correctamente";
        
        System.out.println("✓ Node funciona correctamente");
    }
    
    private static void testSinglyLinkedList() {
        System.out.println("\n--- Test SinglyLinkedList ---");
        SinglyLinkedList<String> list = new SinglyLinkedList<>();
        
        // Test isEmpty y size
        assert list.isEmpty() : "Error: lista debería estar vacía";
        assert list.size() == 0 : "Error: tamaño debería ser 0";
        
        // Test addLast
        list.addLast("A");
        list.addLast("B");
        list.addLast("C");
        assert list.size() == 3 : "Error: tamaño debería ser 3";
        assert !list.isEmpty() : "Error: lista no debería estar vacía";
        
        // Test getFirst y getLast
        assert list.getFirst().equals("A") : "Error: primer elemento debería ser A";
        assert list.getLast().equals("C") : "Error: último elemento debería ser C";
        
        // Test removeFirst
        String first = list.removeFirst();
        assert first.equals("A") : "Error: removeFirst debería retornar A";
        assert list.size() == 2 : "Error: tamaño debería ser 2";
        assert list.getFirst().equals("B") : "Error: nuevo primer elemento debería ser B";
        
        // Test addFirst
        list.addFirst("Z");
        assert list.getFirst().equals("Z") : "Error: primer elemento debería ser Z";
        assert list.size() == 3 : "Error: tamaño debería ser 3";
        
        // Test remove
        boolean removed = list.remove("B");
        assert removed : "Error: remove debería retornar true";
        assert list.size() == 2 : "Error: tamaño debería ser 2";
        
        // Test toArray
        Object[] array = list.toArray();
        assert array.length == 2 : "Error: array debería tener 2 elementos";
        assert array[0].equals("Z") : "Error: array[0] debería ser Z";
        assert array[1].equals("C") : "Error: array[1] debería ser C";
        
        // Test clear
        list.clear();
        assert list.isEmpty() : "Error: lista debería estar vacía después de clear";
        assert list.size() == 0 : "Error: tamaño debería ser 0 después de clear";
        
        System.out.println("✓ SinglyLinkedList funciona correctamente");
    }
    
    private static void testQueue() {
        System.out.println("\n--- Test Queue ---");
        Queue<Integer> queue = new Queue<>();
        
        // Test isEmpty
        assert queue.isEmpty() : "Error: cola debería estar vacía";
        
        // Test enqueue
        queue.enqueue(10);
        queue.enqueue(20);
        queue.enqueue(30);
        assert queue.size() == 3 : "Error: tamaño debería ser 3";
        
        // Test peek
        assert queue.peek() == 10 : "Error: peek debería retornar 10";
        assert queue.size() == 3 : "Error: peek no debería cambiar el tamaño";
        
        // Test dequeue (FIFO)
        assert queue.dequeue() == 10 : "Error: dequeue debería retornar 10";
        assert queue.dequeue() == 20 : "Error: dequeue debería retornar 20";
        assert queue.size() == 1 : "Error: tamaño debería ser 1";
        
        // Test toArray
        queue.enqueue(40);
        Object[] array = queue.toArray();
        assert array.length == 2 : "Error: array debería tener 2 elementos";
        assert (Integer)array[0] == 30 : "Error: array[0] debería ser 30";
        assert (Integer)array[1] == 40 : "Error: array[1] debería ser 40";
        
        System.out.println("✓ Queue funciona correctamente");
    }
    
    private static void testInsertOrdered() {
        System.out.println("\n--- Test insertOrdered ---");
        SinglyLinkedList<Integer> list = new SinglyLinkedList<>();
        
        // Comparador: menor a mayor
        SinglyLinkedList.Comparator<Integer> ascendente = (a, b) -> a < b;
        
        // Insertar desordenado
        list.insertOrdered(30, ascendente);
        list.insertOrdered(10, ascendente);
        list.insertOrdered(50, ascendente);
        list.insertOrdered(20, ascendente);
        list.insertOrdered(40, ascendente);
        
        // Verificar orden
        Object[] array = list.toArray();
        assert array.length == 5 : "Error: debería haber 5 elementos";
        assert (Integer)array[0] == 10 : "Error: array[0] debería ser 10";
        assert (Integer)array[1] == 20 : "Error: array[1] debería ser 20";
        assert (Integer)array[2] == 30 : "Error: array[2] debería ser 30";
        assert (Integer)array[3] == 40 : "Error: array[3] debería ser 40";
        assert (Integer)array[4] == 50 : "Error: array[4] debería ser 50";
        
        System.out.println("✓ insertOrdered funciona correctamente");
        System.out.println("  Lista ordenada: " + list);
    }
}
