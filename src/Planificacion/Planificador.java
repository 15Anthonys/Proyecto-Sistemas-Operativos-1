package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import soplanificacion.Interfaz;

public class Planificador implements Runnable {
    private final SchedulerAlgorithm algo;
    private volatile boolean running = true;
    private volatile int cycleMs = 1000;

    // --- NUEVO: Variables para el gráfico de utilización ---
    private int cyclesBusy = 0;
    private int cyclesIdle = 0;
    private final int SAMPLE_PERIOD = 10; // Trazar un punto en el gráfico cada 10 ciclos
    // --- FIN DE NUEVO ---

    public Planificador(SchedulerAlgorithm algo) { this.algo = algo; }

    public void setCycleMs(int ms) { this.cycleMs = Math.max(1, ms); }
    public int getCycleMs() { return cycleMs; }
    public void detener() { running = false; }

    public void admitirAListos(Process p, long now) {
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        try { algo.enqueue(Interfaz.colaListos, p, now); } 
        finally { Interfaz.semaforoListos.release(); }
    }

    public void cambiarAlgoritmo(SchedulerAlgorithm nuevo) {
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        try {
            Cola<Process> temp = new Cola<>();
            while (!Interfaz.colaListos.isEmpty()) temp.insert(Interfaz.colaListos.pop());
            long now = Interfaz.globalClock.get();
            while (!temp.isEmpty()) nuevo.enqueue(Interfaz.colaListos, temp.pop(), now);
        } finally { Interfaz.semaforoListos.release(); }
    }

    @Override
    public void run() {
        while (running) {
            try { Thread.sleep(cycleMs); } catch (InterruptedException ignored) {}

            // avanza reloj global
            long now = Interfaz.globalClock.incrementAndGet(); 

            // --- NUEVO: Lógica de recolección de datos para el gráfico ---
            // Capturamos el estado ANTES de cualquier lógica de despacho
            if (Interfaz.procesoEnCPU != null) {
                cyclesBusy++;
            } else {
                cyclesIdle++;
            }

            // Revisa si es momento de guardar la muestra
            if ((cyclesBusy + cyclesIdle) >= SAMPLE_PERIOD) {
                // Calcula la utilización en esta ventana de 10 ciclos
                double utilization = ((double) cyclesBusy / (double) (cyclesBusy + cyclesIdle)) * 100.0;
                
                // Añade el dato al gráfico (X=tiempo, Y=utilización)
                // (XYSeries es thread-safe, no necesitamos semáforo para .add)
                Interfaz.seriesUtilizacion.add(now, utilization);

                // Resetea contadores para la próxima ventana
                cyclesBusy = 0;
                cyclesIdle = 0;
            }
            // --- FIN DE NUEVO ---


            // --- Lógica de planificación existente ---

            // Expropiación por política
            Process current = Interfaz.procesoEnCPU;
            if (current != null && algo.isPreemptive()) {
                if (algo.shouldPreempt(current, Interfaz.colaListos, now)) {
                    current.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm) {
                        ((FeedbackAlgorithm) algo).onQuantumExpired(current, now);
                    } else {
                        admitirAListos(current, now);
                    }
                    Interfaz.procesoEnCPU = null;
                }
            }

            // Despacho
            if (Interfaz.procesoEnCPU == null) {
                Process next = null;
                try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); continue; }
                try { next = algo.pickNext(Interfaz.colaListos, now); } 
                finally { Interfaz.semaforoListos.release(); }

                if (next != null) {
                    next.setState(ProcessState.RUNNING);
                    if (next.firstRunCycle < 0) next.firstRunCycle = now;
                    Interfaz.procesoEnCPU = next;
                }
            }

            // Ejecutar 1 ciclo
            if (Interfaz.procesoEnCPU != null) {
                Process r = Interfaz.procesoEnCPU;
                r.step();
                if (r.quantumRemaining > 0) r.quantumRemaining--;

                if (r.shouldRaiseIO() && !r.isFinished()) {
                    Interfaz.procesoEnCPU = null; // Libera la CPU
                    r.setState(ProcessState.BLOCKED);

                    r.setTiempoBloqueadoRestante(r.getExceptionService()); 

                    // Mueve el proceso a la cola de Bloqueados
                    try { Interfaz.semaforoBloqueados.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); continue; }
                    try { Interfaz.colaBloqueados.insert(r); } 
                    finally { Interfaz.semaforoBloqueados.release(); }

                    continue; // Salta al siguiente ciclo
                }

                if (r.isFinished()) {
                    r.setState(ProcessState.TERMINATED);
                    r.finishCycle = now;
                    Interfaz.procesoEnCPU = null;
                    try { Interfaz.semaforoTerminados.acquire(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); continue; }
                    try { Interfaz.colaTerminados.insert(r); } 
                    finally { Interfaz.semaforoTerminados.release(); }
                    Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                    continue;
                }

                if (algo.isPreemptive() && algo.shouldPreempt(r, Interfaz.colaListos, now)) {
                    Interfaz.procesoEnCPU = null;
                    r.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm) {
                        ((FeedbackAlgorithm) algo).onQuantumExpired(r, now);
                    } else {
                        admitirAListos(r, now);
                    }
                }
            }
        }
    }
}