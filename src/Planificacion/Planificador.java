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

    public Planificador(SchedulerAlgorithm algo){ this.algo = algo; }

    public void setCycleMs(int ms){ this.cycleMs = Math.max(1, ms); }
    public int getCycleMs(){ return cycleMs; }
    public void detener(){ running = false; }

    public void admitirAListos(Process p, long now){
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); return; }
        try { algo.enqueue(Interfaz.colaListos, p, now); }
        finally { Interfaz.semaforoListos.release(); }
    }

    public void cambiarAlgoritmo(SchedulerAlgorithm nuevo){
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); return; }
        try {
            Cola<Process> temp = new Cola<>();
            while(!Interfaz.colaListos.isEmpty()) temp.insert(Interfaz.colaListos.pop());
            long now = Interfaz.globalClock.get();
            while(!temp.isEmpty()) nuevo.enqueue(Interfaz.colaListos, temp.pop(), now);
        } finally { Interfaz.semaforoListos.release(); }
    }

    @Override
    public void run() {
        while(running){
            try { Thread.sleep(cycleMs); } catch (InterruptedException ignored){}

            long now = Interfaz.globalClock.incrementAndGet(); // avanza reloj global

            // Expropiación por política
            Process current = Interfaz.procesoEnCPU;
            if (current != null && algo.isPreemptive()){
                if (algo.shouldPreempt(current, Interfaz.colaListos, now)){
                    current.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm){
                        ((FeedbackAlgorithm)algo).onQuantumExpired(current, now);
                    } else {
                        admitirAListos(current, now);
                    }
                    Interfaz.procesoEnCPU = null;
                }
            }

            // Despacho
            if (Interfaz.procesoEnCPU == null){
                Process next = null;
                try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                try { next = algo.pickNext(Interfaz.colaListos, now); }
                finally { Interfaz.semaforoListos.release(); }

                if (next != null){
                    next.setState(ProcessState.RUNNING);
                    if (next.firstRunCycle < 0) next.firstRunCycle = now;
                    Interfaz.procesoEnCPU = next;
                }
            }

            // Ejecutar 1 ciclo
            if (Interfaz.procesoEnCPU != null){
                Process r = Interfaz.procesoEnCPU;
                r.step();
                if (r.quantumRemaining > 0) r.quantumRemaining--;

                if (r.shouldRaiseIO() && !r.isFinished()){
                    Interfaz.procesoEnCPU = null;
                    r.setState(ProcessState.BLOCKED);
                    try { Interfaz.semaforoBloqueados.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                    try { Interfaz.colaBloqueados.insert(r); }
                    finally { Interfaz.semaforoBloqueados.release(); }

                    Thread t = new Thread(() -> {
                        try { Thread.sleep((long) r.getExceptionService() * cycleMs); } catch (InterruptedException ignored){}
                        try { Interfaz.semaforoBloqueados.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); return; }
                        try { Interfaz.colaBloqueados.remove(r); }
                        finally { Interfaz.semaforoBloqueados.release(); }
                        r.setState(ProcessState.READY);
                        r.queueLevel = 0;
                        admitirAListos(r, Interfaz.globalClock.get());
                    }, "IO-P"+r.getId());
                    t.start();
                    continue;
                }

                if (r.isFinished()){
                    r.setState(ProcessState.TERMINATED);
                    r.finishCycle = now;
                    Interfaz.procesoEnCPU = null;
                    try { Interfaz.semaforoTerminados.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                    try { Interfaz.colaTerminados.insert(r); }
                    finally { Interfaz.semaforoTerminados.release(); }
                    Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                    continue;
                }

                if (algo.isPreemptive() && algo.shouldPreempt(r, Interfaz.colaListos, now)){
                    Interfaz.procesoEnCPU = null;
                    r.setState(ProcessState.READY);
                    if (algo instanceof FeedbackAlgorithm){
                        ((FeedbackAlgorithm)algo).onQuantumExpired(r, now);
                    } else {
                        admitirAListos(r, now);
                    }
                }
            }
        }
    }
}