// Asegúrate de importar las clases de Swing si no están ya
import javax.swing.SwingUtilities;
import soplanificacion.Interfaz;

public class SOPlanificacion {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Esta es la forma recomendada para iniciar una GUI de Swing
        // Se asegura de que la interfaz se cree y muestre en el hilo
        // correcto (el Event Dispatch Thread o EDT).
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // 1. Creamos una instancia (un objeto) de tu ventana
                Interfaz ventana = new Interfaz();
                
                // 2. La hacemos visible
                ventana.setVisible(true);
            }
        });
    }
    
    public void Prueba(){
    /**
     * prueba para hacer commit
     * si esi cambios
     */
    }
}