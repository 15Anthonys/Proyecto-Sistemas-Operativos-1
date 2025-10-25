package Planificacion;

import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz;
import EstructurasDeDatos.Nodo;

/**
 * Hilo PMP (VERSIÓN CORREGIDA CON CONTADOR ATÓMICO)
 * Controla Admisión, Suspensión y Reanudación basado en un contador global.
 */
public class PlanificadorMedianoPlazo implements Runnable {
    private volatile boolean running = true;
    private final int MAX_EN_RAM = 5;

    public void detener(){ running = false; }

    @Override
    public void run() {
        while(running){
            try { Thread.sleep(300); } catch (InterruptedException ignored){}
            // Si hay demasiados en RAM, suspender de READY
            int enRam = (int) Interfaz.contadorProcesosEnMemoria.get();
            if (enRam > MAX_EN_RAM){
                // mover 1 de READY a READY SUSPENDIDO
                try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                Process p = null;
                try { p = Interfaz.colaListos.pop(); }
                finally { Interfaz.semaforoListos.release(); }
                if (p != null){
                    p.setState(ProcessState.SUSPENDED_READY);
                    try { Interfaz.semaforoListosSuspendidos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                    try { Interfaz.colaListosSuspendidos.insert(p); }
                    finally { Interfaz.semaforoListosSuspendidos.release(); }
                    Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                }
            } else {
                // Si hay espacio, traer de SUSPENDED_READY a READY
                try { Interfaz.semaforoListosSuspendidos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                Process p = null;
                try { p = Interfaz.colaListosSuspendidos.pop(); }
                finally { Interfaz.semaforoListosSuspendidos.release(); }
                if (p != null){
                    p.setState(ProcessState.READY);
                    Interfaz.contadorProcesosEnMemoria.incrementAndGet();
                    // Encolar por el algoritmo actual (si existe)
                    if (soplanificacion.MotorSimulacion.planificador != null){
                        soplanificacion.MotorSimulacion.planificador.admitirAListos(p, Interfaz.globalClock.get());
                    } else {
                        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); continue; }
                        try { Interfaz.colaListos.insert(p); }
                        finally { Interfaz.semaforoListos.release(); }
                    }
                }
            }
        }
    }
}