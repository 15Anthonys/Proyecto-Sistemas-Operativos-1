package Planificacion;

import EstructurasDeDatos.Cola;
import ProccesFabrication.Process;

public class FCFSAlgorithm implements SchedulerAlgorithm { // Asegúrate que implemente la interfaz

    @Override
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) {
            return null; // Devuelve null si la cola está vacía
        }
        // --- ¡CAMBIO CRÍTICO AQUÍ! ---
        // Usa pop() para SACAR el primer proceso de la cola.
        return colaListos.pop();
        // --- FIN DEL CAMBIO ---
    }

    @Override
    public void setQuantum(int quantum) {
        // FCFS no usa quantum, así que este método no hace nada.
        // Déjalo vacío como lo tienes.
    }
}