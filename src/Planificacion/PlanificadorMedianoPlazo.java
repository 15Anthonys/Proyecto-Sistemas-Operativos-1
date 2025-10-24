package Planificacion;

import EstructurasDeDatos.Cola;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz;
import soplanificacion.MotorSimulacion;
import java.util.concurrent.Semaphore;

/**
 * Hilo PMP (VERSIÓN CORREGIDA CON CONTADOR ATÓMICO)
 * Controla Admisión, Suspensión y Reanudación basado en un contador global.
 */
public class PlanificadorMedianoPlazo implements Runnable {
    
    private boolean simulacionActiva = true;
    private final int MAX_PROCESOS_EN_MEMORIA = 2; // (Sigue en 2)

    // Referencias a todos los semáforos
    private Semaphore semNuevos = Interfaz.semaforoNuevos;
    private Semaphore semListos = Interfaz.semaforoListos;
    private Semaphore semBloqueados = Interfaz.semaforoBloqueados;
    private Semaphore semListosSusp = Interfaz.semaforoListosSuspendidos;
    private Semaphore semBloqueadosSusp = Interfaz.semaforoBloqueadosSuspendidos;

    @Override
    public void run() {
        while (simulacionActiva) {
            try {
                Thread.sleep(500); // Revisa 2 veces por segundo
                
                // <<< ¡CAMBIO CLAVE! --- Lee el contador global ---
                int procesosEnMemoria = Interfaz.contadorProcesosEnMemoria.get();
                
                // --- LÓGICA DE ADMISIÓN (Nuevos -> Listos) ---
                /*
                if (procesosEnMemoria < MAX_PROCESOS_EN_MEMORIA) {
                    Process nuevoProceso = null;
                    semNuevos.acquire();
                    try {
                        if (!Interfaz.colaNuevos.isEmpty()) {
                            nuevoProceso = Interfaz.colaNuevos.pop();
                        }
                    } finally {
                        semNuevos.release();
                    }
                    
                    if (nuevoProceso != null) {
                        System.out.println("(!) PMP: Admitiendo " + nuevoProceso.getName() + " (Nuevos -> RAM)");
                        iniciarHiloProceso(nuevoProceso);
                        reanudarProceso(nuevoProceso, ProcessState.READY, Interfaz.colaListos, semListos);
                        
                        // <<< ¡CAMBIO CLAVE! --- Incrementa el contador ---
                        Interfaz.contadorProcesosEnMemoria.incrementAndGet();

                    }
                }*/
                
                // --- LÓGICA DE SUSPENSIÓN (RAM -> Disco) ---
                if (procesosEnMemoria > MAX_PROCESOS_EN_MEMORIA) {
                    Process victima = null;
                    semListos.acquire();
                    try {
                        if (!Interfaz.colaListos.isEmpty()) {
                            victima = Interfaz.colaListos.pop();
                        }
                    } finally {
                        semListos.release();
                    }
                    
                    if (victima != null) {
                        System.out.println("(!) PMP: Memoria llena. Suspendiendo " + victima.getName() + " (Listos -> Disco)");
                        suspenderProceso(victima, ProcessState.SUSPENDED_READY, Interfaz.colaListosSuspendidos, semListosSusp);

                        // <<< ¡CAMBIO CLAVE! --- Decrementa el contador ---
                        Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                        
                    } else {
                        semBloqueados.acquire();
                        try {
                            if (!Interfaz.colaBloqueados.isEmpty()) {
                                victima = Interfaz.colaBloqueados.pop();
                            }
                        } finally {
                            semBloqueados.release();
                        }
                        
                        if (victima != null) {
                            System.out.println("(!) PMP: Memoria llena. Suspendiendo " + victima.getName() + " (Bloqueados -> Disco)");
                            suspenderProceso(victima, ProcessState.SUSPENDED_BLOCKED, Interfaz.colaBloqueadosSuspendidos, semBloqueadosSusp);
                            
                            // <<< ¡CAMBIO CLAVE! --- Decrementa el contador ---
                            Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                        }
                    }
                }
                
                // --- LÓGICA DE REANUDACIÓN (Disco -> RAM) ---
                if (procesosEnMemoria < MAX_PROCESOS_EN_MEMORIA) {
                    Process reanudado = null;
                    semListosSusp.acquire();
                    try {
                        if (!Interfaz.colaListosSuspendidos.isEmpty()) {
                            reanudado = Interfaz.colaListosSuspendidos.pop();
                        }
                    } finally {
                        semListosSusp.release();
                    }
                    
                    if (reanudado != null) {
                        System.out.println("(!) PMP: Espacio libre. Reanudando " + reanudado.getName() + " (Disco -> RAM)");
                        reanudarProceso(reanudado, ProcessState.READY, Interfaz.colaListos, semListos);
                        
                        // <<< ¡CAMBIO CLAVE! --- Incrementa el contador ---
                        Interfaz.contadorProcesosEnMemoria.incrementAndGet();
                    }
                }
                
            } catch (InterruptedException e) {
                System.out.println("Planificador de Mediano Plazo (PMP) interrumpido.");
                simulacionActiva = false;
            }
        }
    }
    
    // (El resto de la clase: iniciarHiloProceso, suspenderProceso, 
    // reanudarProceso, detener... son idénticos a los que te di antes)
    
    private void iniciarHiloProceso(Process p) throws InterruptedException {
        Thread t = new Thread(p, "Hilo-" + p.getName());
        MotorSimulacion.semaforoHilosProcesos.acquire();
        try {
            MotorSimulacion.hilosProcesos.add(t);
        } finally {
            MotorSimulacion.semaforoHilosProcesos.release();
        }
        t.start();
    }
    
    private void suspenderProceso(Process p, ProcessState nuevoEstado, Cola<Process> colaDestino, Semaphore semDestino) throws InterruptedException {
        p.setState(nuevoEstado); 
        semDestino.acquire();
        try {
            colaDestino.insert(p); 
        } finally {
            semDestino.release();
        }
    }
    
    private void reanudarProceso(Process p, ProcessState nuevoEstado, Cola<Process> colaDestino, Semaphore semDestino) throws InterruptedException {
        p.setState(nuevoEstado); 
        semDestino.acquire();
        try {
            colaDestino.insert(p); 
        } finally {
            semDestino.release();
        }
    }
    
    public void detener() {
        this.simulacionActiva = false;
    }
}