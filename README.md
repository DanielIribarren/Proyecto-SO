# Simulador RTOS para Microsatélite - Proyecto Sistemas Operativos Unimet

## 📋 Descripción
Simulador de Sistema Operativo de Tiempo Real (RTOS) para gestión de microsatélite de investigación.

## 🚀 Cómo abrir en NetBeans

1. Abre NetBeans
2. Ve a **File → Open Project**
3. Navega a: `/Users/danielairibarren/workspace-projects/Proyecto-SO`
4. Selecciona el proyecto y haz clic en **Open Project**
5. NetBeans detectará automáticamente que es un proyecto Maven

## 📁 Estructura del Proyecto

```
Proyecto-SO/
├── src/main/java/
│   ├── ds/                    # Estructuras de datos propias
│   │   ├── Node.java          # Nodo genérico
│   │   ├── SinglyLinkedList.java  # Lista enlazada
│   │   ├── Queue.java         # Cola FIFO
│   │   └── TestDataStructures.java
│   │
│   ├── core/                  # Modelos del dominio
│   │   ├── ProcessState.java # Enum de estados
│   │   ├── Process.java       # PCB completo
│   │   ├── IOSpec.java        # Especificación E/S
│   │   ├── InterruptEvent.java
│   │   └── TestCoreModels.java
│   │
│   ├── sim/                   # Motor de simulación (próximo)
│   ├── metrics/               # Métricas (próximo)
│   └── ui/                    # Interfaz Swing (próximo)
│
└── pom.xml                    # Configuración Maven
```

## ✅ Estado Actual del Desarrollo

### Sprint 1 - COMPLETADO ✓
- ✅ Estructuras de datos propias (sin Collections de Java)

### Sprint 2 - COMPLETADO ✓
- ✅ ProcessState enum (7 estados)
- ✅ InterruptEvent para interrupciones
- ✅ Deadline absoluto implementado

### Sprint 3 - COMPLETADO ✓
- ✅ Motor básico (SimKernel, Clock, EventLog, SystemSnapshot)

### Sprint 4 - COMPLETADO ✓
- ✅ Scheduling con enum Policy (FCFS, RR, SRT, PRIO, EDF)

### Sprint 5 - COMPLETADO ✓
- ✅ E/S y estados avanzados

### Sprint 6 - COMPLETADO ✓
- ✅ Interrupciones con threads
  
### Sprint 7 - COMPLETADO ✓
- ✅ Métricas (tasa de éxito, throughput, CPU utilization)

### Sprint 8 - COMPLETADO ✓
- ✅ GUI Swing

### Sprint 9 - COMPLETADO ✓
- ✅ Testing y documentación para defensa

## 🧪 Cómo ejecutar los tests

### En NetBeans:
1. Expande el paquete `ds` en el explorador de proyectos
2. Click derecho en `TestDataStructures.java` → **Run File**
3. Expande el paquete `core`
4. Click derecho en `TestCoreModels.java` → **Run File**

### Desde terminal (si tienes Maven):
```bash
cd /Users/danielairibarren/workspace-projects/Proyecto-SO
mvn clean compile
mvn exec:java -Dexec.mainClass="ds.TestDataStructures"
mvn exec:java -Dexec.mainClass="core.TestCoreModels"
```

## 🔑 Características Implementadas

### Estructuras de Datos Propias
- **NO usa Collections de Java** (ArrayList, Queue, Stack prohibidos)
- Lista enlazada simple con operaciones O(1) y O(n)
- Cola FIFO wrapper
- Método `toArray()` para snapshots sin violar restricciones
- `insertOrdered()` con Comparator propio para scheduling

### Modelos del Dominio
- **Process:** PCB completo con PID, estado, PC, MAR, prioridad, deadlines
- **ProcessState:** 7 estados (NEW, READY, RUNNING, BLOCKED, SUSPENDED_READY, SUSPENDED_BLOCKED, TERMINATED)
- **IOSpec:** Gestión de E/S con ciclos hasta generar y ciclos para satisfacer
- **InterruptEvent:** Eventos asíncronos con prioridad

### Decisiones Técnicas Clave
1. **Deadline Absoluto:** `absoluteDeadline = arrivalTick + relativeDeadline`
2. **toArray() retorna Object[]:** Correcto para genéricos sin reflexión
3. **ISR con prioridad máxima:** Factory method `Process.createISR()`
4. **Una cola READY:** Con inserción ordenada para políticas (EDF, SRT, PRIO)

## 📚 Requisitos del Proyecto

- **Java:** 21+ (actualmente configurado para Java 23)
- **IDE:** NetBeans
- **Build:** Maven
- **Librerías permitidas:** JFreeChart (gráficas), Hilos/Semáforos estándar
- **Prohibido:** Collections Framework de Java

## 👥 Equipo
Enrique León y Daniel Iribarren 
Proyecto 1 de Sistemas Operativos - Unimet 2526-2
