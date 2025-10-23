package Planificacion;

import EstructurasDeDatos.Cola;
import ProccesFabrication.Process;

// ¿Dice "implements SchedulerAlgorithm"?
public class FCFSAlgorithm implements SchedulerAlgorithm {

    // ¿Tiene @Override? (Ayuda a detectar errores)
    @Override
    // ¿La firma es IDÉNTICA a la de la interfaz?
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) {
            return null;
        }
        // Devuelve el primero sin sacarlo
        return colaListos.getpFirst().getData();
    }

    @Override
    public void setQuantum(int quantum) { /* No usa */ }
}