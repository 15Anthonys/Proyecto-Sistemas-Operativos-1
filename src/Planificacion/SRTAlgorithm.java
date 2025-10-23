package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;

public class SRTAlgorithm implements SchedulerAlgorithm {
    
    @Override
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) return null;

        // --- Lógica de Búsqueda SRT ---
        Nodo<Process> nodoActual = colaListos.getpFirst();
        Process masCorto = nodoActual.getData();
        // ¡Usa getInstruccionesRestantes()!
        int tiempoMinimo = masCorto.getInstruccionesRestantes(); 
        
        while (nodoActual != null) {
            Process p = nodoActual.getData();
            if (p.getInstruccionesRestantes() < tiempoMinimo) {
                tiempoMinimo = p.getInstruccionesRestantes();
                masCorto = p;
            }
            nodoActual = nodoActual.getPnext();
        }
        // --- Fin Búsqueda ---
        
        // TODO: La lógica de *expulsión* de SRT
        // (La implementación completa de SRT es más compleja, 
        // porque el Planificador también tendría que mirar la CPU,
        // pero esto cumple con la *selección*)
        
        return masCorto;
    }
    
    @Override
    public void setQuantum(int quantum) { /* No usa */ }
}