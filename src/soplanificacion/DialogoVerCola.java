// Reemplaza "tu_paquete" con el nombre de tu paquete
package soplanificacion;

// --- Importa tus Clases ---
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process; // ¡Tu clase Process!

// --- Imports de Java Swing ---
import javax.swing.Box;
import javax.swing.JLabel;
import soplanificacion.PanelProcesoVista;

/**
 * JDialog personalizado para mostrar una cola de procesos
 * usando "tarjetas" (PanelProcesoVista) en un scroll horizontal.
 */
public class DialogoVerCola extends javax.swing.JDialog {

    // Variables de la GUI (las creamos aquí porque no usamos el diseñador)
    private javax.swing.JPanel panelContenedorProcesos;
    private javax.swing.JScrollPane scrollPaneContenedor;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea txt;

    /**
     * Constructor
     * @param parent El Frame padre (tu 'Interfaz')
     * @param modal  'true' para que bloquee la ventana principal
     * @param cola   La cola de Procesos que se va a mostrar
     */
    public DialogoVerCola(java.awt.Frame parent, boolean modal, Cola<Process> cola) {
        super(parent, modal);
        
        // 1. Carga los componentes de la GUI (los creamos a mano)
        initComponentsManual();
        
        // 2. Llama al método que llena el panel con tus tarjetas
        llenarPanelConCola(cola);
        
        // 3. Centra la ventana de diálogo
        this.setLocationRelativeTo(parent);
    }

    /**
     * Método que recorre tu Cola<Process> y añade "Tarjetas"
     */
    private void llenarPanelConCola(Cola<Process> cola) {
        
        panelContenedorProcesos.removeAll(); // Limpia
        
        Nodo<Process> actual = cola.getpFirst();
        
        if (actual == null) {
            panelContenedorProcesos.add(new JLabel(" (No hay procesos en esta cola) "));
        } else {
            while (actual != null) {
                Process p = actual.getData(); // Obtiene el Proceso
                
                PanelProcesoVista nuevaTarjeta = new PanelProcesoVista(); // Crea la tarjeta
                nuevaTarjeta.actualizarDatos(p); // ¡Le pasa el Proceso!
                
                panelContenedorProcesos.add(nuevaTarjeta); // Añade al panel
                panelContenedorProcesos.add(Box.createHorizontalStrut(10)); // Espacio
                
                actual = actual.getPnext();
            }
        }
        
        // Refresca el panel
        panelContenedorProcesos.revalidate();
        panelContenedorProcesos.repaint();
    }
    
    /**
     * Este método crea la GUI (en lugar del initComponents de NetBeans)
     */
    private void initComponentsManual() {
        jScrollPane1 = new javax.swing.JScrollPane();
        txt = new javax.swing.JTextArea();
        scrollPaneContenedor = new javax.swing.JScrollPane();
        panelContenedorProcesos = new javax.swing.JPanel();
        btnCerrar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Visor de Cola de Nuevos");

        // --- Configuración del JPanel interno (el que tiene las tarjetas) ---
        // ¡Esta es la línea clave para el scroll horizontal!
        panelContenedorProcesos.setLayout(new javax.swing.BoxLayout(panelContenedorProcesos, javax.swing.BoxLayout.X_AXIS));
        scrollPaneContenedor.setViewportView(panelContenedorProcesos);

        txt.setColumns(30);
        txt.setRows(12);
        txt.setEditable(false);
        jScrollPane1.setViewportView(txt);

        btnCerrar.setText("Cerrar");
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                // Cierra esta ventana de diálogo
                dispose(); 
            }
        });

        // --- Layout del JDialog ---
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPaneContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, 580, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPaneContenedor, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE) // 250 de alto para las tarjetas
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCerrar)
                .addContainerGap())
        );

        pack(); // Ajusta el tamaño
    }
}