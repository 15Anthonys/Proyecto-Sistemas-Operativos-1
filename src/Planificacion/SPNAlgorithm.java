package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;

public class SPNAlgorithm implements SchedulerAlgorithm {
    
    @Override
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) return null;
        
        // --- Lógica de Búsqueda SPN ---
        Nodo<Process> nodoActual = colaListos.getpFirst();
        Process masCorto = nodoActual.getData();
        int duracionMinima = masCorto.getTotalInstructions();
        
        while (nodoActual != null) {
            Process p = nodoActual.getData();
            if (p.getTotalInstructions() < duracionMinima) {
                duracionMinima = p.getTotalInstructions();
                masCorto = p;
            }
            nodoActual = nodoActual.getPnext();
        }
        // --- Fin Búsqueda ---
        
        return masCorto;
    }
    
    @Override
    public void setQuantum(int quantum) { /* No usa */ }
}