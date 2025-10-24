package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz;
import java.util.concurrent.Semaphore;

/**
 * Hilo Planificador de Corto Plazo.
 * Selecciona el siguiente proceso de la cola de Listos según el algoritmo
 * y le asigna el quantum apropiado antes de despertarlo.
 * (Versión corregida para FCFS y RR)
 */
public class Planificador implements Runnable {

    private volatile boolean simulacionActiva = true; // volatile para visibilidad entre hilos
    private SchedulerAlgorithm algoritmo;
    private Semaphore semaforoListos = Interfaz.semaforoListos; // Semáforo Mutex para la cola

    public Planificador(SchedulerAlgorithm algoritmo) {
        this.algoritmo = algoritmo;
    }

    @Override
    public void run() {
        System.out.println("Planificador de Corto Plazo iniciado con algoritmo: " + algoritmo.getClass().getSimpleName());
        while (simulacionActiva && !Thread.currentThread().isInterrupted()) { // Añadir chequeo de interrupción
            try {
                Process proximoProceso = null;

                // 1. Busca y SACA el siguiente proceso de Listos (Zona Crítica)
                //    El algoritmo (FCFS o RR) se encarga de hacer pop().
                semaforoListos.acquire();
                try {
                    proximoProceso = algoritmo.seleccionarSiguiente(Interfaz.colaListos);
                } finally {
                    semaforoListos.release();
                }

                // Si se encontró un proceso
                if (proximoProceso != null) {
                    // 2. Asigna el quantum ANTES de despertar al proceso
                    if (algoritmo instanceof RoundRobinAlgorithm) {
                        // Para RR, el método seleccionarSiguiente ya debería haber asignado el quantum.
                        // Si no lo hace, deberías añadirlo aquí o (mejor) en RoundRobinAlgorithm.java
                        // Ejemplo: proximoProceso.setQuantumRestante(((RoundRobinAlgorithm) algoritmo).getQuantum());
                        if (proximoProceso.getQuantumRestante() <= 0) { // Doble chequeo por si RR no lo asignó
                            System.err.println("Advertencia: RR no asignó quantum a " + proximoProceso.getName() + ". Asignando por defecto.");
                             // Necesitaríamos obtener el quantum del algoritmo aquí. Asumamos 3.
                            proximoProceso.setQuantumRestante(3);
                        }
                    } else {
                        // Para FCFS, SPN, SRT, HRRN (no usan quantum predefinido)
                        proximoProceso.setQuantumRestante(-1); // Señal para "ejecutar sin límite"
                    }

                    // 3. Despierta al hilo del proceso para que compita por la CPU
                    synchronized (proximoProceso) {
                        // El estado se pondrá en RUNNING dentro del hilo del proceso cuando adquiera la CPU
                        proximoProceso.notify();
                    }
                     //System.out.println("Planificador: Despertando a " + proximoProceso.getName()); // Log opcional

                } else {
                    // Si no hay procesos en Listos, el planificador espera un poco
                     Thread.sleep(100); // Espera activa (polling)
                }

                // 4. Pausa breve del planificador (opcional, ya espera si la cola está vacía)
                // Thread.sleep(50); // Puedes ajustar o quitar esto

            } catch (InterruptedException e) {
                System.out.println("Planificador de Corto Plazo interrumpido.");
                simulacionActiva = false;
                Thread.currentThread().interrupt(); // Restablece flag de interrupción
            } catch (Exception e) {
                System.err.println("Error inesperado en Planificador: " + e.getMessage());
                e.printStackTrace();
                simulacionActiva = false; // Detiene el planificador en caso de error grave
            }
        }
         System.out.println("--- Hilo Planificador de Corto Plazo terminado. ---");
    }

    /**
     * Señaliza al hilo para que detenga su bucle principal.
     */
    public void detener() {
        this.simulacionActiva = false;
    }
}