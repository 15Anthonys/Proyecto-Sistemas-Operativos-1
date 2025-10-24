/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package soplanificacion;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import Planificacion.*; // Importa tus clases de Planificacion (Planificador, GestorIO, PMP, Algoritmo)
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;

import javax.swing.*; // Para componentes Swing (Timer, JLabel, etc.)
import java.awt.*; // Para Layouts (BorderLayout)
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author dugla
 */


public class Interfaz extends javax.swing.JFrame {
    
    
    
    
    
    private javax.swing.JScrollPane scrollPaneListos;
    private javax.swing.JPanel panelContenedorListos;
    
    private Timer clockTimer;
    
    
// --- EDD Original (Solo para "Nuevos") ---
    public static Cola<Process> colaNuevos = new Cola<>(); 

    // --- EDD COMPARTIDAS (Protegidas) ---
    // ¡Volvemos a usar Cola.java para todo!
    public static Cola<Process> colaListos = new Cola<>();
    public static Cola<Process> colaBloqueados = new Cola<>();
    public static Cola<Process> colaTerminados = new Cola<>();

    // --- SEMÁFOROS (Los "Candados") ---
    
    public static Semaphore semaforoNuevos = new Semaphore(1);
    // (Estos ya estaban bien inicializados)
    public static Semaphore semaforoListos = new Semaphore(1);
    public static Semaphore semaforoBloqueados = new Semaphore(1);
    public static Semaphore semaforoTerminados = new Semaphore(1);
    public static Semaphore semaforoCPU = new Semaphore(1);
    
    public static Semaphore semContadorListos = new Semaphore(0);
    
    
    public static Cola<Process> colaListosSuspendidos = new Cola<>();
    public static Cola<Process> colaBloqueadosSuspendidos = new Cola<>();
    
    // --- NUEVOS Semáforos de DISCO ---
    public static Semaphore semaforoListosSuspendidos = new Semaphore(1);
    public static Semaphore semaforoBloqueadosSuspendidos = new Semaphore(1);
    
    public static AtomicInteger contadorProcesosEnMemoria = new AtomicInteger(0);
    
    public static AtomicInteger globalClock = new AtomicInteger(0);
    
    public static volatile Process procesoEnCPU = null;

    // ... Hilos ...
    private Thread hiloPlanificador;
    private Thread hiloGestorIO;
    private Planificacion.Planificador planificador;
    private Planificacion.GestorIO gestorIO;
    

// También actualiza el tipo que devuelve el getter
    public EstructurasDeDatos.Cola<ProccesFabrication.Process> getColaNuevos() {
        return colaNuevos;
    }
    
    private Timer guiTimer;
    
    
    private JPanel panelContenedorBloqueados;
    private JPanel panelContenedorListosSusp;
    private JPanel panelContenedorBloqueadosSusp;
    private JPanel panelContenedorTerminados;

    /**
     * Creates new form Interfaz
     */
    public Interfaz() {
        initComponents();
        configurarPanelesDeColasVisualmente();
        actualizarEstadoBotones();
        iniciarRelojGlobal();
        
    }
    
    
    /**
     * Inicia un Timer independiente que actualiza el reloj global (globalClock)
     * y el JLabel TextRelojGlobal cada segundo.
     * LLAMADO DESDE EL CONSTRUCTOR.
     */
    private void iniciarRelojGlobal() {
        // Detiene cualquier timer de reloj anterior si existe
        if (clockTimer != null && clockTimer.isRunning()) {
            clockTimer.stop();
        }

        // Resetea el contador global y el texto del label al inicio
        Interfaz.globalClock.set(0);
        SwingUtilities.invokeLater(() -> TextRelojGlobal.setText("Reloj Global 0"));

        // Crea el Timer que se dispara cada 1000ms (1 segundo)
        clockTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Incrementa el contador atómico global
                int nuevoCiclo = Interfaz.globalClock.incrementAndGet();
                // Actualiza el texto del JLabel en el hilo de Swing
                final String textoReloj = "Reloj Global " + nuevoCiclo;
                SwingUtilities.invokeLater(() -> TextRelojGlobal.setText(textoReloj));
            }
        });

        clockTimer.setInitialDelay(1000); // Espera 1 segundo antes del primer incremento
        clockTimer.setRepeats(true);    // Asegura que se repita
        clockTimer.start();             // ¡Inicia el timer del reloj!

        System.out.println("GUI: Reloj Global iniciado.");
    }
    
    
    private void actualizarEstadoBotones() {
    // Comprueba si hay al menos un proceso en la cola de Nuevos
        boolean hayProcesosNuevos = !colaNuevos.isEmpty();

        // Habilita o deshabilita los botones según el resultado
        btnVerNuevos.setEnabled(hayProcesosNuevos);
        BotonIniciar.setEnabled(hayProcesosNuevos);
}
    
    private void actualizarPanelesDeColas() {
        actualizarUnPanel(panelContenedorListos, colaListos, semaforoListos);
        actualizarUnPanel(panelContenedorBloqueados, colaBloqueados, semaforoBloqueados);
        actualizarUnPanel(panelContenedorListosSusp, colaListosSuspendidos, semaforoListosSuspendidos);
        actualizarUnPanel(panelContenedorBloqueadosSusp, colaBloqueadosSuspendidos, semaforoBloqueadosSuspendidos);
        // --- ¡NUEVA LÍNEA AQUÍ! ---
        actualizarUnPanel(panelContenedorTerminados, colaTerminados, semaforoTerminados);
    }
    
    /**
     * Inicia el Timer de la GUI que refrescará los paneles y el reloj global.
     * Este método debe ser llamado por el ActionListener del botón "Iniciar".
     */
     /**
     * Inicia el Timer de la GUI que refrescará los paneles de colas y CPU
     * periódicamente (cada 250ms).
     * LLAMADO POR BotonIniciarActionPerformed.
     */
    private void iniciarTimerGUI() {
        // Detiene el timer anterior si existe
        if (guiTimer != null && guiTimer.isRunning()) {
            guiTimer.stop();
            System.out.println("GUI: Timer de paneles anterior detenido.");
        }

        // --- YA NO SE RESETEA EL RELOJ GLOBAL AQUÍ ---

        // Crea el Timer que se dispara cada 250ms
        guiTimer = new Timer(250, new ActionListener() {
            // Ya no necesita la variable 'lastClockUpdateTime'

            @Override
            public void actionPerformed(ActionEvent e) {
                // --- SE ELIMINÓ LA LÓGICA DEL RELOJ DE AQUÍ ---

                // --- Actualización de Paneles ---
                actualizarPanelesDeColas(); // Actualiza los 4+1 paneles de colas
                actualizarPanelCPU();     // Actualiza el panel de CPU
                // actualizarPanelTerminadosYEventos(); // (Para después)
                // --- Fin Actualización Paneles ---
            }
        });

        // Configuración adicional del Timer
        guiTimer.setInitialDelay(100); // Pequeña espera antes del primer disparo
        guiTimer.setRepeats(true);    // Asegura que se repita
        guiTimer.start();             // ¡Inicia el Timer de paneles!

        System.out.println("GUI: Timer de paneles iniciado (refresco cada 250ms).");
    }
    
    private void configurarPanelesDeColasVisualmente() {
    configurarUnPanelConScroll(PanelListos, "panelContenedorListos");
    configurarUnPanelConScroll(PanelBloqueados, "panelContenedorBloqueados");
    configurarUnPanelConScroll(PanelListos_Suspendidos, "panelContenedorListosSusp");
    configurarUnPanelConScroll(PanelBloqueados_Suspendidos, "panelContenedorBloqueadosSusp");
    configurarUnPanelConScroll(PanelTerminados, "panelContenedorTerminados");
}
    
    
    private void configurarUnPanelConScroll(JPanel panelExternoExistente, String nombreVariablePanelInterno) {
        // 1. Asegura BorderLayout en el panel gris/azul que ya tienes
        panelExternoExistente.setLayout(new BorderLayout());
        panelExternoExistente.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        // 2. Crea el panel INTERNO (donde irán las TARJETAS)
        JPanel panelInternoNuevo = new JPanel();
        // --- ¡CAMBIO AQUÍ! --- Usa BoxLayout HORIZONTAL ---
        panelInternoNuevo.setLayout(new BoxLayout(panelInternoNuevo, BoxLayout.X_AXIS));
        // --- FIN DEL CAMBIO ---
        panelInternoNuevo.setBackground(Color.WHITE); // Fondo blanco
        panelInternoNuevo.setOpaque(true);

        // 3. Guarda la referencia al panel interno nuevo
        switch (nombreVariablePanelInterno) {
            case "panelContenedorListos":         panelContenedorListos = panelInternoNuevo; break;
            case "panelContenedorBloqueados":     panelContenedorBloqueados = panelInternoNuevo; break;
            case "panelContenedorListosSusp":     panelContenedorListosSusp = panelInternoNuevo; break;
            case "panelContenedorBloqueadosSusp": panelContenedorBloqueadosSusp = panelInternoNuevo; break;
            case "panelContenedorTerminados":     panelContenedorTerminados = panelInternoNuevo; break;
        }

        // 4. Crea el JScrollPane y mete el panel INTERNO dentro
        JScrollPane scrollPane = new JScrollPane(panelInternoNuevo);
        // --- ¡CAMBIO AQUÍ! --- Ajusta las políticas de scroll ---
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // NUNCA vertical
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Horizontal si es necesario
        // --- FIN DEL CAMBIO ---
        scrollPane.setBorder(null);

        // 5. Añade el JScrollPane al CENTRO del panel gris/azul existente
        panelExternoExistente.add(scrollPane, BorderLayout.CENTER);

        // 6. Refresca el panel existente
        panelExternoExistente.revalidate();
        panelExternoExistente.repaint();
    }
    
    
    /**
     * MÉTODO CLAVE: Lee una cola del backend (protegida por semáforo)
     * y crea TARJETAS (PanelProcesoVista) en el panel interno correspondiente. <--- CAMBIO
     */
    private void actualizarUnPanel(JPanel panelInterno, Cola<Process> cola, Semaphore sem) {
        if (panelInterno == null) return;

        // 1. Limpia el panel interno
        panelInterno.removeAll();

        ArrayList<Process> procesosEnCola = new ArrayList<>(); // Lista temporal para Procesos

        try {
            // 2. Intenta adquirir semáforo (rápido)
            if (sem.tryAcquire(50, TimeUnit.MILLISECONDS)) {
                try {
                    // --- SECCIÓN CRÍTICA ---
                    // Copia los OBJETOS Process a la lista temporal
                    Nodo<Process> actual = cola.getpFirst();
                    while (actual != null) {
                        procesosEnCola.add(actual.getData()); // Copia el objeto Process
                        actual = actual.getPnext();
                    }
                } finally {
                    // 3. Libera el semáforo rápido
                    sem.release();
                }
            } else {
                // Semáforo ocupado, no hacemos nada más que limpiar y refrescar
                panelInterno.add(new JLabel(" ...Cargando... ")); // Muestra mensaje temporal
                panelInterno.revalidate();
                panelInterno.repaint();
                return; // Sal del método hasta el siguiente tick del timer
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            panelInterno.add(new JLabel(" Error Hilo "));
            panelInterno.revalidate();
            panelInterno.repaint();
            return; // Sal del método
        }

        // 4. Fuera de la sección crítica, actualiza la GUI con los datos copiados
        if (procesosEnCola.isEmpty()) {
            panelInterno.add(Box.createHorizontalGlue()); // Empuja el texto al centro horizontal
            panelInterno.add(new JLabel(" Vacío "));
            panelInterno.add(Box.createHorizontalGlue());
        } else {
            // --- ¡CAMBIO AQUÍ! --- Crea y añade TARJETAS ---
            panelInterno.add(Box.createHorizontalStrut(5)); // Pequeño margen izquierdo
            for (Process p : procesosEnCola) {
                PanelProcesoVista tarjeta = new PanelProcesoVista(); // Crea tu tarjeta
                tarjeta.actualizarDatos(p); // Le pasas el proceso
                panelInterno.add(tarjeta); // Añades la tarjeta
                panelInterno.add(Box.createHorizontalStrut(5)); // Espacio entre tarjetas
            }
            // --- FIN DEL CAMBIO ---
        }

        // 5. Refresca el panel interno para mostrar las tarjetas
        panelInterno.revalidate();
        panelInterno.repaint();
    }
    
    private void actualizarPanelListos() {
    
        // 1. Apunta al panel que está dentro del JScrollPane
        panelContenedorListos.removeAll();

        // 2. Lee la cola de Listos
        Nodo<ProccesFabrication.Process> actual = colaListos.getpFirst();

        if (actual == null) {
            // Si está vacía, muestra un mensaje
            panelContenedorListos.add(new JLabel(" (Cola de Listos Vacía) "));
        } else {
            // 3. Recorre la cola y crea las tarjetas
            while (actual != null) {
                ProccesFabrication.Process p = actual.getData();

                // Crea una nueva instancia de tu "tarjeta"
                PanelProcesoVista nuevaTarjeta = new PanelProcesoVista();

                // Le pasa el Proceso para que muestre sus datos
                nuevaTarjeta.actualizarDatos(p);

                // Añade la tarjeta al panel horizontal
                panelContenedorListos.add(nuevaTarjeta);

                // Añade un pequeño espacio entre tarjetas
                panelContenedorListos.add(Box.createHorizontalStrut(5));

                actual = actual.getPnext();
            }
        }

        // 4. ¡Muy importante! Refresca el panel para que muestre los cambios
        panelContenedorListos.revalidate();
        panelContenedorListos.repaint();
}
    
    
    /**
     * Selecciona y devuelve la instancia del algoritmo de planificación
     * basado en la selección del JComboBox.
     */
    private Planificacion.SchedulerAlgorithm seleccionarAlgoritmo(String nombreAlgoritmo) {
        switch (nombreAlgoritmo) {
            case "First-Come, First-Served":
                return new Planificacion.FCFSAlgorithm(); // Asumiendo que existe
            case "Round Robin":
                // Necesitas un quantum. Podrías tener un JTextField para configurarlo,
                // o un valor por defecto. Usaremos 3 por ahora.
                return new Planificacion.RoundRobinAlgorithm(3);
            case "Shortest Process Next":
                return new Planificacion.SPNAlgorithm(); // Asumiendo que existe
            case "Shortest Remaining Time":
                return new Planificacion.SRTAlgorithm(); // Asumiendo que existe
            case "Highest Response-Ratio Next":
                return new Planificacion.HRRNAlgorithm(); // Asumiendo que existe
                
            case "Feedback":
                // --- ¡ARREGLO AQUÍ! --- Comenta o elimina este bloque ---
                 System.out.println("ALGORITMO FEEDBACK NO IMPLEMENTADO AÚN");
                 return null; // Devuelve null temporalmente
                 /*
                 // Cuando tengas FeedbackAlgorithm.java en el paquete Planificacion, descomenta:
                 return new Planificacion.FeedbackAlgorithm();
                 */
                 // --- FIN DEL ARREGLO ---
            default:
                JOptionPane.showMessageDialog(this, "Algoritmo no reconocido: " + nombreAlgoritmo, "Error", JOptionPane.ERROR_MESSAGE);
                return null; // O lanza una excepción
        }
    }
    
    /**
     * Actualiza los JLabels del panel de ejecución de CPU
     * basado en la variable global Interfaz.procesoEnCPU.
     * LLAMADO POR EL TIMER.
     */
    private void actualizarPanelCPU() {
        Process p = Interfaz.procesoEnCPU; // Lee la referencia al proceso actual

        // Actualiza los JLabels. Si p es null, muestra "N/A" o "System".
        if (p != null) {
            // Hay un proceso de usuario en ejecución
            lblNombreProcesoCPU.setText(p.getName());
            lblIdProcesoCPU.setText(String.valueOf(p.getId()));
            lblPcProcesoCPU.setText(String.valueOf(p.getProgramCounter()));
            lblMarProcesoCPU.setText(String.valueOf(p.getMemoryAddressRegister())); // O como obtengas el MAR
            lblStatusProcesoCPU.setText(p.getState().toString()); // Debería ser "Ejecutando"
            lblTipoProcesoCPU.setText(p.isIsIOBound() ? "I/O Bound" : "CPU Bound");
        } else {
            // No hay proceso de usuario, CPU idle o ejecutando OS
            lblNombreProcesoCPU.setText("Proceso System"); // O "Idle"
            lblIdProcesoCPU.setText("N/A");
            lblPcProcesoCPU.setText("N/A");
            lblMarProcesoCPU.setText("N/A");
            lblStatusProcesoCPU.setText("N/A"); // O "Idle"
            lblTipoProcesoCPU.setText("N/A"); // O "Kernel"
        }

        // (Opcional) Refrescar el panel si es necesario, aunque cambiar texto de JLabel suele ser automático
        // PanelProcesoEjecucion.revalidate();
        // PanelProcesoEjecucion.repaint();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        TextRelojGlobal = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        BotonReiniciar = new javax.swing.JButton();
        BotonIniciar = new javax.swing.JButton();
        BotonPausar = new javax.swing.JButton();
        jComboAlgoritmos = new javax.swing.JComboBox<>();
        jLabel17 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        BotonCrearProceso = new javax.swing.JButton();
        btnVerNuevos = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        PanelBloqueados_Suspendidos = new javax.swing.JPanel();
        PanelBloqueados = new javax.swing.JPanel();
        PanelListos_Suspendidos = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        PanelListos = new javax.swing.JPanel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        PanelProcesoEjecucion = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        lblNombreProcesoCPU = new javax.swing.JLabel();
        lblIdProcesoCPU = new javax.swing.JLabel();
        lblPcProcesoCPU = new javax.swing.JLabel();
        lblMarProcesoCPU = new javax.swing.JLabel();
        lblStatusProcesoCPU = new javax.swing.JLabel();
        lblTipoProcesoCPU = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        PanelTerminadosEventos = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        PanelRendimiento = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel27 = new javax.swing.JLabel();
        BotonCambiarAlgoritmo = new javax.swing.JButton();
        jLabel28 = new javax.swing.JLabel();
        cycleDurationSlider = new javax.swing.JSlider();
        jPanel16 = new javax.swing.JPanel();
        jLabel24 = new javax.swing.JLabel();
        BotonEscribir = new javax.swing.JButton();
        PanelTerminados = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel7.setBackground(new java.awt.Color(204, 102, 0));
        jPanel7.setBorder(new javax.swing.border.MatteBorder(null));

        TextRelojGlobal.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 14)); // NOI18N
        TextRelojGlobal.setForeground(new java.awt.Color(255, 255, 255));
        TextRelojGlobal.setText("Reloj Global: ");

        jLabel12.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Algoritmo:");

        BotonReiniciar.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonReiniciar.setText("Reiniciar");

        BotonIniciar.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonIniciar.setText("Iniciar");
        BotonIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonIniciarActionPerformed(evt);
            }
        });

        BotonPausar.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonPausar.setText("Pausar");
        BotonPausar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonPausarActionPerformed(evt);
            }
        });

        jComboAlgoritmos.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jComboAlgoritmos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "First-Come, First-Served", "Round Robin", "Shortest Process Next", "Shortest Remaining Time", "Highest Response-Ratio Next", "Feedback" }));
        jComboAlgoritmos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboAlgoritmosActionPerformed(evt);
            }
        });

        jLabel17.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("Control y Estado del Sistema");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addComponent(BotonIniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BotonPausar, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BotonReiniciar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 52, Short.MAX_VALUE)
                        .addComponent(TextRelojGlobal, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboAlgoritmos, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                    .addContainerGap(201, Short.MAX_VALUE)
                    .addComponent(jLabel17)
                    .addGap(152, 152, 152)))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel7Layout.createSequentialGroup()
                        .addGap(61, 61, 61)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboAlgoritmos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8)
                        .addComponent(TextRelojGlobal, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 18, Short.MAX_VALUE))
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(BotonIniciar)
                            .addComponent(BotonPausar)
                            .addComponent(BotonReiniciar))))
                .addGap(95, 95, 95))
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel7Layout.createSequentialGroup()
                    .addGap(16, 16, 16)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(192, Short.MAX_VALUE)))
        );

        jPanel1.add(jPanel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 10, 610, 260));

        jPanel2.setBackground(new java.awt.Color(0, 204, 51));
        jPanel2.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel13.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Creación de Proceso");

        BotonCrearProceso.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonCrearProceso.setText("Crear");
        BotonCrearProceso.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonCrearProcesoActionPerformed(evt);
            }
        });

        btnVerNuevos.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        btnVerNuevos.setText("Ver Cola de Nuevos");
        btnVerNuevos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerNuevosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(87, 87, 87)
                        .addComponent(BotonCrearProceso, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnVerNuevos, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel13))))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(BotonCrearProceso, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnVerNuevos, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 10, 270, 140));

        jPanel3.setBackground(new java.awt.Color(0, 102, 204));
        jPanel3.setBorder(new javax.swing.border.MatteBorder(null));

        PanelBloqueados_Suspendidos.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelBloqueados_Suspendidos.setLayout(new java.awt.BorderLayout());

        PanelBloqueados.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelBloqueados.setLayout(new java.awt.BorderLayout());

        PanelListos_Suspendidos.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelListos_Suspendidos.setLayout(new java.awt.BorderLayout());

        jLabel16.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Bloqueados");

        PanelListos.setLayout(new java.awt.BorderLayout());

        jLabel22.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel22.setForeground(new java.awt.Color(255, 255, 255));
        jLabel22.setText("Visualización de Colas");

        jLabel23.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel23.setForeground(new java.awt.Color(255, 255, 255));
        jLabel23.setText("Listos");

        jLabel25.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel25.setForeground(new java.awt.Color(255, 255, 255));
        jLabel25.setText("Listos/Suspendidos");

        jLabel26.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel26.setForeground(new java.awt.Color(255, 255, 255));
        jLabel26.setText("Bloqueados/Suspendidos");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(119, 119, 119)
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel16)
                .addGap(132, 132, 132))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(196, 196, 196)
                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(75, 75, 75)
                                .addComponent(jLabel25))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(PanelListos, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(PanelListos_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(48, 48, 48)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel26)
                            .addComponent(PanelBloqueados_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(PanelBloqueados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(69, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jLabel23))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(PanelBloqueados, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(PanelListos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(PanelBloqueados_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(PanelListos_Suspendidos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 270, 610, 460));

        jPanel9.setBackground(new java.awt.Color(204, 0, 204));
        jPanel9.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel18.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 255, 255));
        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel18.setText("<html><center>Cpu y Proceso en Ejecucion</center></html>");
        jLabel18.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel1.setFont(new java.awt.Font("Segoe UI Emoji", 1, 14)); // NOI18N
        jLabel1.setText("CPU");

        jLabel2.setText("Proceso en Ejecucion:");

        jLabel3.setText("ID:");

        jLabel4.setText("PC:");

        jLabel5.setText("Status:");

        jLabel6.setText("MAR:");

        jLabel7.setText("Tipo:");

        lblNombreProcesoCPU.setText("N/A");

        lblIdProcesoCPU.setText("N/A");

        lblPcProcesoCPU.setText("N/A");

        lblMarProcesoCPU.setText("N/A");

        lblStatusProcesoCPU.setText("N/A");

        lblTipoProcesoCPU.setText("N/A");

        javax.swing.GroupLayout PanelProcesoEjecucionLayout = new javax.swing.GroupLayout(PanelProcesoEjecucion);
        PanelProcesoEjecucion.setLayout(PanelProcesoEjecucionLayout);
        PanelProcesoEjecucionLayout.setHorizontalGroup(
            PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblNombreProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblTipoProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
                        .addGap(8, 8, 8))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblIdProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblPcProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblMarProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE))
                    .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblStatusProcesoCPU, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(91, 91, 91))
        );
        PanelProcesoEjecucionLayout.setVerticalGroup(
            PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelProcesoEjecucionLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblNombreProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblIdProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPcProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 8, Short.MAX_VALUE)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblMarProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblStatusProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(PanelProcesoEjecucionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTipoProcesoCPU, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9))
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(PanelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(PanelProcesoEjecucion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 150, 270, 300));

        jPanel11.setBackground(new java.awt.Color(102, 102, 102));
        jPanel11.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel19.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("<html><cenater>Log de Eventos</center></html>");
        jLabel19.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        PanelTerminadosEventos.setPreferredSize(new java.awt.Dimension(223, 174));

        javax.swing.GroupLayout PanelTerminadosEventosLayout = new javax.swing.GroupLayout(PanelTerminadosEventos);
        PanelTerminadosEventos.setLayout(PanelTerminadosEventosLayout);
        PanelTerminadosEventosLayout.setHorizontalGroup(
            PanelTerminadosEventosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 197, Short.MAX_VALUE)
        );
        PanelTerminadosEventosLayout.setVerticalGroup(
            PanelTerminadosEventosLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 188, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel19)
                    .addComponent(PanelTerminadosEventos, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE))
                .addGap(36, 36, 36))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(PanelTerminadosEventos, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jLabel19.getAccessibleContext().setAccessibleDescription("");

        jPanel1.add(jPanel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 450, 270, 280));

        jPanel13.setBackground(new java.awt.Color(0, 153, 153));
        jPanel13.setBorder(new javax.swing.border.MatteBorder(null));

        javax.swing.GroupLayout PanelRendimientoLayout = new javax.swing.GroupLayout(PanelRendimiento);
        PanelRendimiento.setLayout(PanelRendimientoLayout);
        PanelRendimientoLayout.setHorizontalGroup(
            PanelRendimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 362, Short.MAX_VALUE)
        );
        PanelRendimientoLayout.setVerticalGroup(
            PanelRendimientoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 259, Short.MAX_VALUE)
        );

        jLabel21.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel21.setForeground(new java.awt.Color(255, 255, 255));
        jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel21.setText("<html><center>Grafico de Rendimiento</center></html>");
        jLabel21.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(PanelRendimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel13Layout.createSequentialGroup()
                        .addGap(72, 72, 72)
                        .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 305, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(49, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(PanelRendimiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 150, 450, 330));

        jPanel15.setBackground(new java.awt.Color(153, 153, 255));
        jPanel15.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel20.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel20.setForeground(new java.awt.Color(255, 255, 255));
        jLabel20.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel20.setText("<html><center>Tiempo de Ejecucion en tiempo real</center></html>");
        jLabel20.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jLabel27.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel27.setForeground(new java.awt.Color(255, 255, 255));
        jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel27.setText("<html><center>Cambio de ciclo de ejecucion</center></html>");
        jLabel27.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        BotonCambiarAlgoritmo.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        BotonCambiarAlgoritmo.setText("Cambiar");
        BotonCambiarAlgoritmo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BotonCambiarAlgoritmoActionPerformed(evt);
            }
        });

        jLabel28.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel28.setForeground(new java.awt.Color(255, 255, 255));
        jLabel28.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel28.setText("<html><center>Intercambiar Algoritmo</center></html>");
        jLabel28.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        cycleDurationSlider.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 12)); // NOI18N
        cycleDurationSlider.setMajorTickSpacing(1000);
        cycleDurationSlider.setMaximum(5000);
        cycleDurationSlider.setMinimum(500);
        cycleDurationSlider.setMinorTickSpacing(500);
        cycleDurationSlider.setPaintLabels(true);
        cycleDurationSlider.setPaintTicks(true);
        cycleDurationSlider.setSnapToTicks(true);
        cycleDurationSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cycleDurationSlider.setEnabled(false);
        cycleDurationSlider.setOpaque(true);
        cycleDurationSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cycleDurationSliderStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(64, 64, 64)
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(BotonCambiarAlgoritmo)))
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addGap(61, 61, 61)
                        .addComponent(cycleDurationSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 11, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(40, 40, 40))))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel28, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BotonCambiarAlgoritmo)
                        .addGap(12, 12, 12))
                    .addGroup(jPanel15Layout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cycleDurationSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(15, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 10, 450, 140));

        jPanel16.setBackground(new java.awt.Color(204, 0, 153));
        jPanel16.setBorder(new javax.swing.border.MatteBorder(null));

        jLabel24.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 12)); // NOI18N
        jLabel24.setForeground(new java.awt.Color(255, 255, 255));
        jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel24.setText("<html><center>Escritura en JSON/CSV</center></html>");
        jLabel24.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        BotonEscribir.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        BotonEscribir.setText("Escribir");

        PanelTerminados.setPreferredSize(new java.awt.Dimension(223, 174));
        PanelTerminados.setLayout(new java.awt.BorderLayout());

        jLabel29.setFont(new java.awt.Font("UD Digi Kyokasho NP", 0, 18)); // NOI18N
        jLabel29.setForeground(new java.awt.Color(255, 255, 255));
        jLabel29.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel29.setText("<html><cenater>Terminados</center></html>");
        jLabel29.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(PanelTerminados, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(51, 51, 51))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                        .addComponent(BotonEscribir, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(41, 41, 41))))
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel16Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(BotonEscribir)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(PanelTerminados, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1.add(jPanel16, new org.netbeans.lib.awtextra.AbsoluteConstraints(900, 480, 450, 250));

        jTabbedPane1.addTab("Simulador", jPanel1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1388, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 746, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Estadisticas", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1388, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void BotonPausarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonPausarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BotonPausarActionPerformed

    private void BotonCrearProcesoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonCrearProcesoActionPerformed
        CrearProcesoDialog dialogo = new CrearProcesoDialog(this, true);
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);

        // --- AÑADE ESTA LÍNEA ---
        // Después de que el diálogo se cierra, actualiza el estado de los botones
        actualizarEstadoBotones();

    // El código aquí se ejecutará DESPUÉS de que el diálogo se haya cerrado.
    // Aquí podrías, por ejemplo, recuperar los datos que el usuario introdujo.
    }//GEN-LAST:event_BotonCrearProcesoActionPerformed

    private void BotonCambiarAlgoritmoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonCambiarAlgoritmoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BotonCambiarAlgoritmoActionPerformed

    private void btnVerNuevosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerNuevosActionPerformed
        // TODO add your handling code here:
        // 1. Crea la nueva ventana de diálogo, pasándole:
    //    'this'     (la Interfaz principal)
    //    'true'     (para que sea modal)
    //    'colaNuevos' (tu cola que tiene los objetos Process)
        DialogoVerCola dialogo = new DialogoVerCola(this, true, this.colaNuevos);

        // 2. La hace visible
        dialogo.setVisible(true);
    }//GEN-LAST:event_btnVerNuevosActionPerformed

    private void BotonIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BotonIniciarActionPerformed
        // TODO add your handling code here:
            // 1. Lee la selección del JComboBox
            String nombreAlgoritmoSeleccionado = (String) jComboAlgoritmos.getSelectedItem();
            Planificacion.SchedulerAlgorithm algoritmo = seleccionarAlgoritmo(nombreAlgoritmoSeleccionado); // Usa el método que te di antes

            // Verifica si se pudo crear el algoritmo
            if (algoritmo == null) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo iniciar la simulación.\nAlgoritmo '" + nombreAlgoritmoSeleccionado + "' no reconocido o no implementado.",
                        "Error de Algoritmo", JOptionPane.ERROR_MESSAGE);
                return; // Detiene la acción si el algoritmo no es válido
            }

            final Planificacion.SchedulerAlgorithm algoritmoFinal = algoritmo; // Necesario para usar dentro del hilo lambda

            // 2. Cuenta cuántos procesos hay en Nuevos (de forma segura)
            int totalProcesos;
            try {
                // Intenta adquirir el semáforo brevemente para no bloquear la GUI
                if (!Interfaz.semaforoNuevos.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                     JOptionPane.showMessageDialog(this, "Error: No se pudo acceder a la cola de Nuevos.", "Error de Concurrencia", JOptionPane.ERROR_MESSAGE);
                     return; // No se pudo contar, no inicia
                }
                try {
                    totalProcesos = Interfaz.colaNuevos.getSize();
                } finally {
                    Interfaz.semaforoNuevos.release(); // Libera el semáforo
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura el estado interrumpido
                JOptionPane.showMessageDialog(this, "Error al contar procesos iniciales.", "Error de Hilo", JOptionPane.ERROR_MESSAGE);
                return; // No inicia si hubo interrupción
            }

            // Verifica si hay procesos para simular
            if (totalProcesos == 0) {
                JOptionPane.showMessageDialog(this, "No hay procesos en la cola de 'Nuevos' para iniciar.", "Simulación Vacía", JOptionPane.WARNING_MESSAGE);
                return; // No inicia si no hay procesos
            }

            // --- ¡Inicio de la Simulación! ---
            System.out.println("GUI: Iniciando simulación con " + totalProcesos + " procesos y algoritmo " + nombreAlgoritmoSeleccionado + "...");

            // 3. Lanza el MOTOR DE SIMULACIÓN (tu ConsoleSimulator/MotorSimulacion) en un HILO NUEVO
            //    Esto es CRUCIAL para que la GUI no se congele.
            new Thread(() -> {
                // Llama a los métodos ESTÁTICOS de tu clase MotorSimulacion
                // Asegúrate de usar la versión con PMP controlando admisión (la última que te di)
                MotorSimulacion.iniciarSimulacion(algoritmoFinal);   // Inicia los hilos PMP, RR, GestorIO
                MotorSimulacion.esperarFinSimulacion(totalProcesos); // Espera a que terminen los procesos
                MotorSimulacion.detenerSimulacion();                 // Detiene los hilos auxiliares

                // --- Simulación Terminada ---
                // Código a ejecutar DESPUÉS de que el motor termine.
                // Debe ejecutarse en el hilo de la GUI (EDT).
                SwingUtilities.invokeLater(() -> {
                    if (guiTimer != null) {
                        guiTimer.stop(); // Detiene el refresco de la GUI
                        System.out.println("GUI: Timer detenido.");
                    }
                    actualizarEstadoBotones(); // Reactiva botones Iniciar/Crear
                    System.out.println("GUI: Simulación finalizada en el backend.");
                    JOptionPane.showMessageDialog(Interfaz.this, // Usa Interfaz.this como parent
                            "Simulación completada con éxito.",
                            "Simulación Terminada", JOptionPane.INFORMATION_MESSAGE);
                    // Refresca los paneles una última vez para mostrar el estado final
                    actualizarPanelesDeColas();
                });
            }, "HiloMotorSimulacion").start(); // Inicia el hilo del motor

            // 4. Inicia el TIMER de la GUI (el que llama a actualizarPanelesDeColas cada 250ms)
            iniciarTimerGUI();

            // 5. Actualiza el estado de los botones (Deshabilita Iniciar, Crear, etc.)
            actualizarEstadoBotones();
    }//GEN-LAST:event_BotonIniciarActionPerformed

    private void jComboAlgoritmosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboAlgoritmosActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboAlgoritmosActionPerformed

    private void cycleDurationSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cycleDurationSliderStateChanged
        // TODO add your handling code here:
        //int newSpeed = this.cycleDurationSlider.getValue();
        //System.out.println(newSpeed);
        //getOperatingSystem().getSystemClock().setCycleDuration(newSpeed);
    }//GEN-LAST:event_cycleDurationSliderStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Interfaz.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Interfaz().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BotonCambiarAlgoritmo;
    private javax.swing.JButton BotonCrearProceso;
    private javax.swing.JButton BotonEscribir;
    private javax.swing.JButton BotonIniciar;
    private javax.swing.JButton BotonPausar;
    private javax.swing.JButton BotonReiniciar;
    private javax.swing.JPanel PanelBloqueados;
    private javax.swing.JPanel PanelBloqueados_Suspendidos;
    private javax.swing.JPanel PanelListos;
    private javax.swing.JPanel PanelListos_Suspendidos;
    private javax.swing.JPanel PanelProcesoEjecucion;
    private javax.swing.JPanel PanelRendimiento;
    private javax.swing.JPanel PanelTerminados;
    private javax.swing.JPanel PanelTerminadosEventos;
    private javax.swing.JLabel TextRelojGlobal;
    private javax.swing.JButton btnVerNuevos;
    private javax.swing.JSlider cycleDurationSlider;
    private javax.swing.JComboBox<String> jComboAlgoritmos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblIdProcesoCPU;
    private javax.swing.JLabel lblMarProcesoCPU;
    private javax.swing.JLabel lblNombreProcesoCPU;
    private javax.swing.JLabel lblPcProcesoCPU;
    private javax.swing.JLabel lblStatusProcesoCPU;
    private javax.swing.JLabel lblTipoProcesoCPU;
    // End of variables declaration//GEN-END:variables
}
