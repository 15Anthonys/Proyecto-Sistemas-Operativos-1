package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz; // Importa tu Interfaz para acceder a las colas

/**
 * Este es un HILO que se ejecuta constantemente.
 * Reemplaza al "Timer". Su trabajo es despertar al siguiente proceso
 * de la cola de Listos.
 */
public class Planificador implements Runnable {
    
    private boolean simulacionActiva = true;
    private SchedulerAlgorithm algoritmo; // La estrategia (FCFS, RR...)

    public Planificador(SchedulerAlgorithm algoritmo) {
        this.algoritmo = algoritmo;
    }

    @Override
    public void run() {
        while (simulacionActiva) {
            try {
                // 1. Busca en la cola de Listos
                Process proximoProceso = algoritmo.seleccionarSiguiente(Interfaz.colaListos);
                
                if (proximoProceso != null) {
                    // 2. Si encontró uno, lo "despierta"
                    synchronized (proximoProceso) {
                        proximoProceso.setState(ProcessState.READY); // Lo marca
                        proximoProceso.notify(); // ¡Despierta, hilo!
                    }
                }
                
                // 3. Duerme un "tic" para no saturar la CPU
                Thread.sleep(100); // El "tic" del planificador
                
            } catch (InterruptedException e) {
                System.out.println("Planificador interrumpido.");
                simulacionActiva = false;
            }
        }
    }
    
    public void detener() {
        this.simulacionActiva = false;
    }
}