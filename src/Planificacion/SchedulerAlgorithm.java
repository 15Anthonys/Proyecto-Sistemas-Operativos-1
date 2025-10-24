package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;

public interface SchedulerAlgorithm {
    String name();
    boolean isPreemptive();
    void setQuantum(int q);
    int getQuantum();
    // Encolar en estructura del algoritmo (puede usar cola global)
    void enqueue(Cola<Process> ready, Process p, long now);
    // Seleccionar siguiente desde cola(s)
    Process pickNext(Cola<Process> ready, long now);
    // ¿Debe expropiar al actual?
    boolean shouldPreempt(Process running, Cola<Process> ready, long now);
}