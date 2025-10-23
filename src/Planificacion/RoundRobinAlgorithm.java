package Planificacion;

import EstructurasDeDatos.Cola;
import ProccesFabrication.Process;

public class RoundRobinAlgorithm implements SchedulerAlgorithm {

    private int quantum;
    
    public RoundRobinAlgorithm(int quantum) {
        this.quantum = quantum;
    }
    
    @Override
    public Process seleccionarSiguiente(Cola<Process> colaListos) {
        if (colaListos.isEmpty()) {
            return null;
        }
        
        // --- ARREGLO AQUÍ ---
        // Cambiamos "getpFirst().getData()" por "pop()"
        // pop() SÍ remueve el proceso de la cola.
        Process p = colaListos.pop(); 
        // --- FIN DEL ARREGLO ---
        
        // ¡Le asigna su quantum antes de devolverlo!
        p.setQuantumRestante(this.quantum); 
        return p;
    }

    @Override
    public void setQuantum(int quantum) {
        this.quantum = quantum;
    }
}