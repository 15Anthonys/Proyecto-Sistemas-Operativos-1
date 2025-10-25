package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import soplanificacion.Interfaz;
import javax.swing.SwingUtilities; // Asegúrate de tener esta importación

public class Planificador implements Runnable {
    public volatile SchedulerAlgorithm algo; // 'volatile' y público (como dijiste)
    private volatile boolean running = true;
    private volatile int cycleMs = 1000;

    // --- Variables para el gráfico de utilización ---
    private int cyclesBusy = 0;
    private int cyclesIdle = 0;
    private final int SAMPLE_PERIOD = 10;

    public Planificador(SchedulerAlgorithm algo) {
        this.algo = algo;
    }

    public void setCycleMs(int ms) { this.cycleMs = Math.max(1, ms); }
    public int getCycleMs() { return cycleMs; }
    public void detener() { running = false; }

    /**
     * Añade un proceso a la cola de Listos (método seguro).
     */
    public void admitirAListos(Process p, long now) {
        try {
            Interfaz.semaforoListos.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            // --- LOG ---
            Interfaz.logEvento("LCP: Proceso " + p.getName() + " admitido a cola 'Listos'.");
            algo.enqueue(Interfaz.colaListos, p, now);
        } finally {
            Interfaz.semaforoListos.release();
        }
    }

    /**
     * Cambia el algoritmo en tiempo de ejecución (método seguro).
     */
    public void cambiarAlgoritmo(SchedulerAlgorithm nuevo) {
        Interfaz.logEvento("LCP: Solicitud de cambio de algoritmo a " + nuevo.getClass().getSimpleName());
        try {
            Interfaz.semaforoListos.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            Cola<Process> temp = new Cola<>();
            while (!Interfaz.colaListos.isEmpty()) {
                temp.insert(Interfaz.colaListos.pop());
            }
            long now = Interfaz.globalClock.get();
            while (!temp.isEmpty()) {
                nuevo.enqueue(Interfaz.colaListos, temp.pop(), now);
            }
            this.algo = nuevo; // Actualiza el algoritmo
            Interfaz.logEvento("LCP: Algoritmo cambiado exitosamente.");
        } finally {
            Interfaz.semaforoListos.release();
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(cycleMs);
            } catch (InterruptedException ignored) {
            }

            // avanza reloj global
            long now = Interfaz.globalClock.incrementAndGet();

            // --- Lógica de recolección de datos para el gráfico ---
            if (Interfaz.procesoEnCPU != null) {
                cyclesBusy++;
            } else {
                cyclesIdle++;
            }

            if ((cyclesBusy + cyclesIdle) >= SAMPLE_PERIOD) {
                final double utilization = ((double) cyclesBusy / (double) (cyclesBusy + cyclesIdle)) * 100.0;
                final long timeForGraph = now;
                SwingUtilities.invokeLater(() -> {
                    Interfaz.seriesUtilizacion.add(timeForGraph, utilization);
                });
                cyclesBusy = 0;
                cyclesIdle = 0;
            }
            // --- Fin Lógica Gráfico ---


            // --- Lógica de planificación ---

            // Expropiación por política (ej. SRT)
            Process current = Interfaz.procesoEnCPU;
            if (current != null && algo.isPreemptive()) {
                if (algo.shouldPreempt(current, Interfaz.colaListos, now)) {
                    // --- LOG ---
                    Interfaz.logEvento("LCP: Expropiando " + current.getName() + " (Política: " + algo.getClass().getSimpleName() + ").");
                    current.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm) {
                        ((FeedbackAlgorithm) algo).onQuantumExpired(current, now);
                    } else {
                        admitirAListos(current, now);
                    }
                    Interfaz.procesoEnCPU = null;
                }
            }

            // Despacho (si la CPU está libre)
            if (Interfaz.procesoEnCPU == null) {
                Process next = null;
                try {
                    Interfaz.semaforoListos.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                try {
                    next = algo.pickNext(Interfaz.colaListos, now);
                } finally {
                    Interfaz.semaforoListos.release();
                }

                if (next != null) {
                    // --- LOG ---
                    Interfaz.logEvento("LCP: Despachado Proceso " + next.getName() + " a CPU.");
                    next.setState(ProcessState.RUNNING);
                    if (next.firstRunCycle < 0) next.firstRunCycle = now;
                    Interfaz.procesoEnCPU = next;
                } else {
                    // --- LOG ---
                    // (Este log solo aparecerá una vez y luego se repetirá por el "cyclesIdle")
                    if (cyclesIdle == 1) { // Loguea solo en el primer ciclo inactivo
                         Interfaz.logEvento("LCP: CPU Inactiva. No hay procesos en 'Listos'.");
                    }
                }
            }

            // Ejecutar 1 ciclo del proceso en CPU
            if (Interfaz.procesoEnCPU != null) {
                Process r = Interfaz.procesoEnCPU;
                r.step(); // Ejecuta 1 instrucción
                if (r.quantumRemaining > 0) r.quantumRemaining--;

                // Revisar si genera I/O
                if (r.shouldRaiseIO() && !r.isFinished()) {
                    // --- LOG ---
                    Interfaz.logEvento("LCP: Proceso " + r.getName() + " bloqueado por I/O. Moviendo a 'Bloqueados'.");
                    Interfaz.procesoEnCPU = null; // Libera la CPU
                    r.setState(ProcessState.BLOCKED);
                    r.setTiempoBloqueadoRestante(r.getExceptionService());

                    // Mueve a 'Bloqueados'
                    try {
                        Interfaz.semaforoBloqueados.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    try {
                        Interfaz.colaBloqueados.insert(r);
                    } finally {
                        Interfaz.semaforoBloqueados.release();
                    }
                    continue; // Salta al siguiente ciclo
                }

                // Revisar si terminó
                if (r.isFinished()) {
                    // --- LOG ---
                    Interfaz.logEvento("LCP: Proceso " + r.getName() + " TERMINADO. Moviendo a 'Terminados'.");
                    r.setState(ProcessState.TERMINATED);
                    r.finishCycle = now;
                    Interfaz.procesoEnCPU = null;
                    try {
                        Interfaz.semaforoTerminados.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    try {
                        Interfaz.colaTerminados.insert(r);
                    } finally {
                        Interfaz.semaforoTerminados.release();
                    }
                    Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                    continue;
                }

                // Revisar si se le acabó el Quantum (RR, Feedback)
                if (algo.isPreemptive() && algo.shouldPreempt(r, Interfaz.colaListos, now)) {
                    // --- LOG ---
                    Interfaz.logEvento("LCP: Quantum de " + r.getName() + " expirado. Moviendo a 'Listos'.");
                    Interfaz.procesoEnCPU = null;
                    r.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm) {
                        ((FeedbackAlgorithm) algo).onQuantumExpired(r, now);
                    } else {
                        admitirAListos(r, now);
                    }
                }
            }
        } // fin while(running)
        Interfaz.logEvento("LCP: Hilo detenido.");
    } // fin run()
}