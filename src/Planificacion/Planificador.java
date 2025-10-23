package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz; 
// Importa el semáforo también
import java.util.concurrent.Semaphore; // <<< CAMBIO

public class Planificador implements Runnable {
    
    private boolean simulacionActiva = true;
    private SchedulerAlgorithm algoritmo;
    
    // Referencia al semáforo MUTEX de la colaListos
    private Semaphore semaforoListos = Interfaz.semaforoListos; // <<< CAMBIO

    public Planificador(SchedulerAlgorithm algoritmo) {
        this.algoritmo = algoritmo;
    }

    @Override
    public void run() {
        while (simulacionActiva) {
            try {
                Process proximoProceso = null; // <<< CAMBIO: Declarar fuera

                // 1. Busca en la cola de Listos (ZONA CRÍTICA)
                try {
                    semaforoListos.acquire(); // <<< CAMBIO: Bloquear el semáforo
                    
                    // Esta línea ahora es segura
                    proximoProceso = algoritmo.seleccionarSiguiente(Interfaz.colaListos); 
                
                } finally {
                    semaforoListos.release(); // <<< CAMBIO: Liberar el semáforo
                }
                // --- Fin de la Zona Crítica ---

                
                if (proximoProceso != null) {
                    // 2. Si encontró uno, lo "despierta"
                    // (Es bueno hacer el notify fuera del lock)
                    synchronized (proximoProceso) {
                        proximoProceso.setState(ProcessState.READY);
                        proximoProceso.notify(); // ¡Despierta, hilo!
                    }
                }
                
                // 3. Duerme un "tic" 
                Thread.sleep(100); 
                
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