package ui;

import core.IOSpec;
import core.InterruptEvent;
import core.SimProcess;
import metrics.MetricsCollector;
import sim.Clock;
import sim.Policy;
import sim.SimKernel;
import sim.SystemSnapshot;
import sim.SystemSnapshot.ProcessInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

// Ventana principal del simulador RTOS
// Toda la UI se construye en código (sin .form) para máxima portabilidad
public class MainWindow extends JFrame {
    
    // Kernel y control
    private SimKernel kernel;
    private Timer uiTimer;
    private boolean simulationRunning;
    private int tickDelay; // ms entre ticks
    private int nextPid;
    
    // Componentes principales
    private JLabel lblClock;
    private JLabel lblIndicator; // SO vs Usuario
    private JLabel lblPolicyDisplay;
    
    // Tablas de colas
    private JTable tableReady;
    private JTable tableBlocked;
    private JTable tableSuspendedReady;
    private JTable tableSuspendedBlocked;
    private JTable tableTerminated;
    
    // Panel de proceso en ejecución
    private JLabel lblRunningPid;
    private JLabel lblRunningName;
    private JLabel lblRunningState;
    private JLabel lblRunningPC;
    private JLabel lblRunningMAR;
    private JLabel lblRunningPriority;
    private JLabel lblRunningDeadline;
    
    // Log
    private JTextArea txtLog;
    
    // Métricas
    private JLabel lblCpuUtil;
    private JLabel lblSuccessRate;
    private JLabel lblThroughput;
    private JLabel lblAvgWait;
    private JLabel lblAvgTurnaround;
    private JLabel lblCompleted;
    private JLabel lblMissedDeadlines;
    
    // Gráfico
    private XYSeries cpuSeries;
    
    // Controles
    private JComboBox<Policy> cmbPolicy;
    private JSpinner spnQuantum;
    private JSlider sldSpeed;
    private JButton btnStartPause;
    private JButton btnStep;
    private JToggleButton btnInterrupts;
    
    public MainWindow() {
        super("Simulador RTOS - Microsatélite de Investigación");
        
        this.kernel = new SimKernel();
        this.simulationRunning = false;
        this.tickDelay = 500; // 500ms por defecto
        this.nextPid = 1;
        
        initComponents();
        setupTimer();
        generateInitialProcesses();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        
        // === PANEL SUPERIOR: Reloj + Indicador + Política ===
        add(createTopPanel(), BorderLayout.NORTH);
        
        // === PANEL CENTRAL: Colas + Running + Log ===
        add(createCenterPanel(), BorderLayout.CENTER);
        
        // === PANEL DERECHO: Métricas + Gráfico ===
        add(createRightPanel(), BorderLayout.EAST);
        
        // === PANEL INFERIOR: Controles ===
        add(createBottomPanel(), BorderLayout.SOUTH);
    }
    
    // ==================== PANEL SUPERIOR ====================
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        // Reloj global
        lblClock = new JLabel("Ciclo de Reloj: 0");
        lblClock.setFont(new Font("Monospaced", Font.BOLD, 18));
        panel.add(lblClock);
        
        // Indicador SO vs Usuario
        lblIndicator = new JLabel("  IDLE  ");
        lblIndicator.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblIndicator.setOpaque(true);
        lblIndicator.setBackground(Color.LIGHT_GRAY);
        lblIndicator.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));
        panel.add(lblIndicator);
        
        // Política actual
        lblPolicyDisplay = new JLabel("Política: FCFS");
        lblPolicyDisplay.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(lblPolicyDisplay);
        
        return panel;
    }
    
    // ==================== PANEL CENTRAL ====================
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Parte superior: Colas en pestañas
        JTabbedPane tabbedQueues = new JTabbedPane();
        
        tableReady = createProcessTable();
        tableBlocked = createProcessTable();
        tableSuspendedReady = createProcessTable();
        tableSuspendedBlocked = createProcessTable();
        tableTerminated = createProcessTable();
        
        tabbedQueues.addTab("Ready", new JScrollPane(tableReady));
        tabbedQueues.addTab("Blocked", new JScrollPane(tableBlocked));
        tabbedQueues.addTab("Susp. Ready", new JScrollPane(tableSuspendedReady));
        tabbedQueues.addTab("Susp. Blocked", new JScrollPane(tableSuspendedBlocked));
        tabbedQueues.addTab("Terminados", new JScrollPane(tableTerminated));
        
        // Parte media: Proceso en ejecución
        JPanel runningPanel = createRunningPanel();
        
        // Parte inferior: Log de eventos
        txtLog = new JTextArea(8, 40);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        
        // Dividir: colas arriba, running + log abajo
        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedQueues, runningPanel);
        topSplit.setResizeWeight(0.6);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, logScroll);
        mainSplit.setResizeWeight(0.7);
        
        panel.add(mainSplit, BorderLayout.CENTER);
        return panel;
    }
    
    private JTable createProcessTable() {
        String[] columns = {"PID", "Nombre", "Estado", "PC", "MAR", "Prioridad", "Instr. Rest.", "Deadline Rest."};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        
        // Centrar contenido
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        return table;
    }
    
    private JPanel createRunningPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Proceso en Ejecución (PCB)"));
        
        lblRunningPid = new JLabel("PID: -");
        lblRunningName = new JLabel("Nombre: -");
        lblRunningState = new JLabel("Estado: -");
        lblRunningPC = new JLabel("PC: -");
        lblRunningMAR = new JLabel("MAR: -");
        lblRunningPriority = new JLabel("Prioridad: -");
        lblRunningDeadline = new JLabel("Deadline Rest.: -");
        
        Font pcbFont = new Font("Monospaced", Font.PLAIN, 12);
        lblRunningPid.setFont(pcbFont);
        lblRunningName.setFont(pcbFont);
        lblRunningState.setFont(pcbFont);
        lblRunningPC.setFont(pcbFont);
        lblRunningMAR.setFont(pcbFont);
        lblRunningPriority.setFont(pcbFont);
        lblRunningDeadline.setFont(pcbFont);
        
        panel.add(lblRunningPid);
        panel.add(lblRunningName);
        panel.add(lblRunningState);
        panel.add(lblRunningPC);
        panel.add(lblRunningMAR);
        panel.add(lblRunningPriority);
        panel.add(lblRunningDeadline);
        panel.add(new JLabel("")); // espacio
        
        return panel;
    }
    
    // ==================== PANEL DERECHO ====================
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
        
        // Métricas arriba
        JPanel metricsPanel = new JPanel(new GridLayout(7, 1, 2, 2));
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Métricas"));
        
        lblCpuUtil = new JLabel("CPU Utilización: 0.0%");
        lblSuccessRate = new JLabel("Tasa de Éxito: 0.0%");
        lblThroughput = new JLabel("Throughput: 0.000");
        lblAvgWait = new JLabel("Espera Prom.: 0.0");
        lblAvgTurnaround = new JLabel("Turnaround Prom.: 0.0");
        lblCompleted = new JLabel("Completados: 0");
        lblMissedDeadlines = new JLabel("Deadlines Perdidos: 0");
        
        Font metricFont = new Font("SansSerif", Font.PLAIN, 12);
        lblCpuUtil.setFont(metricFont);
        lblSuccessRate.setFont(metricFont);
        lblThroughput.setFont(metricFont);
        lblAvgWait.setFont(metricFont);
        lblAvgTurnaround.setFont(metricFont);
        lblCompleted.setFont(metricFont);
        lblMissedDeadlines.setFont(metricFont);
        
        metricsPanel.add(lblCpuUtil);
        metricsPanel.add(lblSuccessRate);
        metricsPanel.add(lblThroughput);
        metricsPanel.add(lblAvgWait);
        metricsPanel.add(lblAvgTurnaround);
        metricsPanel.add(lblCompleted);
        metricsPanel.add(lblMissedDeadlines);
        
        // Gráfico abajo
        cpuSeries = new XYSeries("CPU %");
        XYSeriesCollection dataset = new XYSeriesCollection(cpuSeries);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Utilización CPU vs Tiempo",
            "Tick",
            "CPU %",
            dataset,
            PlotOrientation.VERTICAL,
            false, false, false
        );
        chart.getXYPlot().getRangeAxis().setRange(0, 105);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        chartPanel.setBorder(BorderFactory.createTitledBorder("Gráfico CPU"));
        
        panel.add(metricsPanel, BorderLayout.NORTH);
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    // ==================== PANEL INFERIOR ====================
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        // Start/Pause
        btnStartPause = new JButton("Iniciar");
        btnStartPause.addActionListener(this::onStartPause);
        panel.add(btnStartPause);
        
        // Step
        btnStep = new JButton("Step");
        btnStep.addActionListener(this::onStep);
        panel.add(btnStep);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Política
        panel.add(new JLabel("Política:"));
        cmbPolicy = new JComboBox<>(Policy.values());
        cmbPolicy.setSelectedItem(Policy.FCFS);
        cmbPolicy.addActionListener(this::onPolicyChange);
        panel.add(cmbPolicy);
        
        // Quantum
        panel.add(new JLabel("Quantum:"));
        spnQuantum = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        spnQuantum.addChangeListener(e -> {
            int val = (int) spnQuantum.getValue();
            kernel.setQuantum(val);
        });
        panel.add(spnQuantum);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Velocidad
        panel.add(new JLabel("Velocidad (ms):"));
        sldSpeed = new JSlider(50, 2000, 500);
        sldSpeed.setPreferredSize(new Dimension(120, 25));
        sldSpeed.addChangeListener(e -> {
            tickDelay = sldSpeed.getValue();
            if (uiTimer != null) {
                uiTimer.setDelay(tickDelay);
            }
        });
        panel.add(sldSpeed);
        
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Generar 20 procesos
        JButton btnGenerate20 = new JButton("Generar 20 Procesos");
        btnGenerate20.addActionListener(this::onGenerate20);
        panel.add(btnGenerate20);
        
        // Agregar 1 proceso
        JButton btnAdd1 = new JButton("Agregar Proceso");
        btnAdd1.addActionListener(this::onAddProcess);
        panel.add(btnAdd1);
        
        // Toggle interrupciones automáticas
        btnInterrupts = new JToggleButton("Activar Interrupciones");
        btnInterrupts.addActionListener(this::onToggleInterrupts);
        panel.add(btnInterrupts);
        
        // Interrupción de emergencia manual
        JButton btnEmergency = new JButton("Emergencia");
        btnEmergency.setForeground(Color.RED);
        btnEmergency.addActionListener(this::onEmergencyInterrupt);
        panel.add(btnEmergency);
        
        return panel;
    }
    
    // ==================== TIMER Y SIMULACIÓN ====================
    
    private void setupTimer() {
        uiTimer = new Timer(tickDelay, e -> {
            if (simulationRunning) {
                kernel.tick();
                refreshUI();
            }
        });
    }
    
    private void generateInitialProcesses() {
        // Configuración inicial dinámica: generar conjunto inicial
        for (int i = 0; i < 5; i++) {
            SimProcess p = createRandomProcess();
            kernel.addProcess(p);
        }
        // Hacer un tick inicial para generar snapshot
        kernel.tick();
        refreshUI();
    }
    
    // ==================== REFRESH UI ====================
    
    private void refreshUI() {
        SystemSnapshot snap = kernel.getSnapshot();
        if (snap == null) return;
        
        // Reloj
        lblClock.setText("Ciclo de Reloj: " + snap.currentTick);
        
        // Indicador SO vs Usuario
        if (snap.running == null) {
            lblIndicator.setText("  IDLE  ");
            lblIndicator.setBackground(Color.LIGHT_GRAY);
        } else if (snap.running.isISR) {
            lblIndicator.setText("  SISTEMA OPERATIVO  ");
            lblIndicator.setBackground(new Color(255, 100, 100)); // rojo
        } else {
            lblIndicator.setText("  PROGRAMA DE USUARIO  ");
            lblIndicator.setBackground(new Color(100, 200, 100)); // verde
        }
        
        // Política
        lblPolicyDisplay.setText("Política: " + snap.currentPolicy);
        
        // Tablas de colas
        updateTable(tableReady, snap.readyQueue);
        updateTable(tableBlocked, snap.blockedQueue);
        updateTable(tableSuspendedReady, snap.suspendedReady);
        updateTable(tableSuspendedBlocked, snap.suspendedBlocked);
        updateTable(tableTerminated, snap.terminated);
        
        // Proceso en ejecución
        if (snap.running != null) {
            ProcessInfo r = snap.running;
            lblRunningPid.setText("PID: " + r.pid);
            lblRunningName.setText("Nombre: " + r.name);
            lblRunningState.setText("Estado: " + r.state);
            lblRunningPC.setText("PC: " + r.pc);
            lblRunningMAR.setText("MAR: " + r.mar);
            lblRunningPriority.setText("Prioridad: " + r.priority);
            if (r.deadlineRemaining < 0) {
                lblRunningDeadline.setText("Deadline: MISSED (" + Math.abs(r.deadlineRemaining) + " tarde)");
                lblRunningDeadline.setForeground(Color.RED);
            } else {
                lblRunningDeadline.setText("Deadline Rest.: " + r.deadlineRemaining);
                lblRunningDeadline.setForeground(Color.BLACK);
            }
        } else {
            lblRunningPid.setText("PID: -");
            lblRunningName.setText("Nombre: -");
            lblRunningState.setText("Estado: -");
            lblRunningPC.setText("PC: -");
            lblRunningMAR.setText("MAR: -");
            lblRunningPriority.setText("Prioridad: -");
            lblRunningDeadline.setText("Deadline Rest.: -");
            lblRunningDeadline.setForeground(Color.BLACK);
        }
        
        // Log de eventos
        StringBuilder logText = new StringBuilder();
        String[] logs = snap.logs;
        // Mostrar últimos 50 eventos
        int start = Math.max(0, logs.length - 50);
        for (int i = start; i < logs.length; i++) {
            logText.append(logs[i]).append("\n");
        }
        txtLog.setText(logText.toString());
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
        
        // Métricas
        lblCpuUtil.setText(String.format("CPU Utilización: %.1f%%", snap.cpuUtilization));
        lblSuccessRate.setText(String.format("Tasa de Éxito: %.1f%%", snap.successRate));
        lblThroughput.setText(String.format("Throughput: %.3f proc/tick", snap.throughput));
        lblAvgWait.setText(String.format("Espera Prom.: %.1f ticks", snap.averageWaitTime));
        lblAvgTurnaround.setText(String.format("Turnaround Prom.: %.1f ticks", snap.averageTurnaroundTime));
        lblCompleted.setText("Completados: " + snap.completedProcesses);
        lblMissedDeadlines.setText("Deadlines Perdidos: " + snap.missedDeadlines);
        
        // Gráfico (limitar a últimos 300 puntos)
        cpuSeries.add(snap.currentTick, snap.cpuUtilization);
        if (cpuSeries.getItemCount() > 300) {
            cpuSeries.remove(0);
        }
        
        // Auto-pausar si no hay procesos en ninguna cola
        if (simulationRunning && snap.running == null 
                && snap.readyQueue.length == 0 && snap.blockedQueue.length == 0
                && snap.suspendedReady.length == 0 && snap.suspendedBlocked.length == 0) {
            simulationRunning = false;
            uiTimer.stop();
            btnStartPause.setText("Reanudar");
            btnStep.setEnabled(true);
            lblIndicator.setText("  SIMULACIÓN FINALIZADA  ");
            lblIndicator.setBackground(Color.ORANGE);
        }
    }
    
    private void updateTable(JTable table, ProcessInfo[] processes) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        for (ProcessInfo p : processes) {
            String deadlineStr = p.deadlineRemaining < 0 
                ? "MISSED (" + Math.abs(p.deadlineRemaining) + ")" 
                : String.valueOf(p.deadlineRemaining);
            model.addRow(new Object[]{
                p.pid,
                p.name,
                p.state,
                p.pc,
                p.mar,
                p.priority,
                p.instructionsRemaining,
                deadlineStr
            });
        }
    }
    
    // ==================== HANDLERS ====================
    
    private void onStartPause(ActionEvent e) {
        simulationRunning = !simulationRunning;
        
        if (simulationRunning) {
            btnStartPause.setText("Pausar");
            btnStep.setEnabled(false);
            uiTimer.start();
        } else {
            btnStartPause.setText("Reanudar");
            btnStep.setEnabled(true);
            uiTimer.stop();
        }
    }
    
    private void onStep(ActionEvent e) {
        if (!simulationRunning) {
            kernel.tick();
            refreshUI();
        }
    }
    
    private void onPolicyChange(ActionEvent e) {
        Policy selected = (Policy) cmbPolicy.getSelectedItem();
        if (selected != null) {
            kernel.setPolicy(selected);
            refreshUI();
        }
    }
    
    private void onToggleInterrupts(ActionEvent e) {
        if (btnInterrupts.isSelected()) {
            kernel.startInterruptGenerator(2000, 6000);
            btnInterrupts.setText("Desactivar Interrupciones");
        } else {
            kernel.stopInterruptGenerator();
            btnInterrupts.setText("Activar Interrupciones");
        }
    }
    
    private void onGenerate20(ActionEvent e) {
        for (int i = 0; i < 20; i++) {
            SimProcess p = createRandomProcess();
            kernel.addProcess(p);
        }
        refreshUI();
    }
    
    private void onAddProcess(ActionEvent e) {
        // Diálogo para agregar proceso con validaciones
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        
        JTextField txtName = new JTextField("Proceso_" + nextPid);
        JSpinner spnInstructions = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        JSpinner spnPriority = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        JSpinner spnDeadline = new JSpinner(new SpinnerNumberModel(50, 5, 1000, 5));
        JCheckBox chkIO = new JCheckBox("Tiene E/S");
        
        inputPanel.add(new JLabel("Nombre:"));
        inputPanel.add(txtName);
        inputPanel.add(new JLabel("Instrucciones (1-500):"));
        inputPanel.add(spnInstructions);
        inputPanel.add(new JLabel("Prioridad (1-100):"));
        inputPanel.add(spnPriority);
        inputPanel.add(new JLabel("Deadline relativo (5-1000):"));
        inputPanel.add(spnDeadline);
        inputPanel.add(new JLabel("E/S:"));
        inputPanel.add(chkIO);
        
        int result = JOptionPane.showConfirmDialog(this, inputPanel, 
            "Agregar Proceso", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío.", 
                    "Error de validación", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int instructions = (int) spnInstructions.getValue();
            int priority = (int) spnPriority.getValue();
            int deadline = (int) spnDeadline.getValue();
            int currentTick = kernel.getClock().getCurrentTick();
            
            SimProcess p = new SimProcess(nextPid++, name, instructions, priority, currentTick, deadline);
            
            if (chkIO.isSelected()) {
                int cyclesUntil = 1 + (int)(Math.random() * (instructions / 2));
                int cyclesToSatisfy = 2 + (int)(Math.random() * 5);
                p.setIoSpec(new IOSpec(cyclesUntil, cyclesToSatisfy));
            }
            
            kernel.addProcess(p);
            refreshUI();
        }
    }
    
    private void onEmergencyInterrupt(ActionEvent e) {
        int currentTick = kernel.getClock().getCurrentTick();
        InterruptEvent emergency = new InterruptEvent(
            "EMERGENCY", 10, currentTick, "Interrupción de emergencia manual"
        );
        kernel.addInterrupt(emergency);
        
        // Si no está corriendo, hacer un tick para procesar
        if (!simulationRunning) {
            kernel.tick();
        }
        refreshUI();
    }
    
    // ==================== UTILIDADES ====================
    
    private SimProcess createRandomProcess() {
        int pid = nextPid++;
        String[] names = {
            "Telemetria", "Navegacion", "Comunicacion", "Sensor_Temp",
            "Sensor_Alt", "Camara", "GPS", "Bateria", "Panel_Solar",
            "Antena", "Giroscopio", "Magnetometro", "Propulsion",
            "Datos_Cientificos", "Compresion", "Transmision", "Recepcion",
            "Calibracion", "Diagnostico", "Monitoreo"
        };
        String name = names[(int)(Math.random() * names.length)] + "_" + pid;
        int instructions = 5 + (int)(Math.random() * 30);
        int priority = 1 + (int)(Math.random() * 10);
        int currentTick = kernel.getClock().getCurrentTick();
        int deadline = instructions + 10 + (int)(Math.random() * 40);
        
        SimProcess p = new SimProcess(pid, name, instructions, priority, currentTick, deadline);
        
        // 30% de probabilidad de tener E/S
        if (Math.random() < 0.3) {
            int cyclesUntil = 1 + (int)(Math.random() * (instructions / 2));
            int cyclesToSatisfy = 2 + (int)(Math.random() * 5);
            p.setIoSpec(new IOSpec(cyclesUntil, cyclesToSatisfy));
        }
        
        return p;
    }
    
    // ==================== MAIN ====================
    
    public static void main(String[] args) {
        // Usar Look and Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si falla, usar el default
        }
        
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
