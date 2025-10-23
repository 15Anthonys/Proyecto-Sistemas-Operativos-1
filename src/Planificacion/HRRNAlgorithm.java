package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;

public class HRRNAlgorithm implements SchedulerAlgorithm {
    
    @Override
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) return null;
            
        // --- Lógica de Búsqueda HRRN ---
        Nodo<Process> nodoActual = colaListos.getpFirst();
        Process mejorProceso = nodoActual.getData();
        double maxRatio = -1.0;
        
        while (nodoActual != null) {
            Process p = nodoActual.getData();
            
            // R = (w + s) / s
            // w = ciclosEnEspera (¡Debes incrementarlo en el GestorIO o Planificador!)
            // s = totalInstructions
            
            double w = (double) p.getCiclosEnEspera();
            double s = (double) p.getTotalInstructions();
            if (s == 0) s = 1; // Evitar división por cero
            
            double ratio = (w + s) / s;
            
            if (ratio > maxRatio) {
                maxRatio = ratio;
                mejorProceso = p;
            }
            nodoActual = nodoActual.getPnext();
        }
        // --- Fin Búsqueda ---
        
        mejorProceso.setCiclosEnEspera(0); // Resetea el contador
        return mejorProceso;
    }
    
    @Override
    public void setQuantum(int quantum) { /* No usa */ }
}