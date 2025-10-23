package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola; // ¡Usa tu EDD!

public interface SchedulerAlgorithm {
    
    /**
     * Revisa la cola de listos y DEVUELVE al proceso que
     * debe ejecutarse a continuación.
     * ¡Importante: Este método NO lo saca de la cola!
     */
    Process seleccionarSiguiente(Cola<Process> colaListos);
    
    void setQuantum(int quantum);
}