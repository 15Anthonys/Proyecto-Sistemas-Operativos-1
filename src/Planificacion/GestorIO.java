package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz; // Importa la Interfaz para el log

/**
 * Hilo que gestiona AMBAS colas de Bloqueados (RAM y Disco).
 * (VERSIÓN ACTUALIZADA CON LOGS)
 */
public class GestorIO implements Runnable {
    
    private boolean simulacionActiva = true;
    
    // Listas temporales (locales a este hilo, no necesitan semáforo)
    private ListaSimple<Process> procesosListosRAM;
    private ListaSimple<Process> procesosListosDisco; // <<< NUEVO

    public GestorIO() {
        this.procesosListosRAM = new ListaSimple<>();
        this.procesosListosDisco = new ListaSimple<>(); // <<< NUEVO
    }
    
    @Override
    public void run() {
        while (simulacionActiva) {
            try {
                // 1. Duerme un "ciclo" de E/S
                // (Reducido a 500ms para que sea un poco más rápido que el planificador)
                Thread.sleep(500); 
                
                // --- PARTE 1: MARCAR (RAM) ---
                Interfaz.semaforoBloqueados.acquire();
                try {
                    marcarProcesosTerminados(Interfaz.colaBloqueados, procesosListosRAM);
                } finally {
                    Interfaz.semaforoBloqueados.release();
                }
                
                // --- PARTE 2: MARCAR (DISCO) ---
                Interfaz.semaforoBloqueadosSuspendidos.acquire();
                try {
                    marcarProcesosTerminados(Interfaz.colaBloqueadosSuspendidos, procesosListosDisco);
                } finally {
                    Interfaz.semaforoBloqueadosSuspendidos.release();
                }
                
                // --- PARTE 3: MOVER (RAM) ---
                // Mueve de Bloqueados (RAM) -> Listos (RAM)
                while (!procesosListosRAM.isEmpty()) {
                    Process p = procesosListosRAM.getValueByIndex(0);
                    procesosListosRAM.deleteFirst();

                    // 5a. Saca de Bloqueados (RAM)
                    Interfaz.semaforoBloqueados.acquire();
                    try {
                        Interfaz.colaBloqueados.remove(p);
                    } finally {
                        Interfaz.semaforoBloqueados.release();
                    }
                    
                    // 5b. Mete en Listos (RAM)
                    Interfaz.semaforoListos.acquire();
                    try {
                        p.setState(ProcessState.READY);
                        Interfaz.colaListos.insert(p);
                        
                        // --- LOG --- (Reemplaza System.out)
                        Interfaz.logEvento("GestorIO: " + p.getName() + " (RAM) -> Listos (RAM).");
                        
                    } finally {
                        Interfaz.semaforoListos.release();
                    }

                    // 5c. ¡¡DESPIERTA AL HILO!!
                    synchronized (p) {
                        p.notify();
                    }
                }

                // --- PARTE 4: MOVER (DISCO) ---
                // Mueve de Bloqueados/Susp (Disco) -> Listos/Susp (Disco)
                while (!procesosListosDisco.isEmpty()) {
                    Process p = procesosListosDisco.getValueByIndex(0);
                    procesosListosDisco.deleteFirst();

                    // 6a. Saca de Bloqueados/Susp (Disco)
                    Interfaz.semaforoBloqueadosSuspendidos.acquire();
                    try {
                        Interfaz.colaBloqueadosSuspendidos.remove(p);
                    } finally {
                        Interfaz.semaforoBloqueadosSuspendidos.release();
                    }
                    
                    // 6b. Mete en Listos/Susp (Disco)
                    Interfaz.semaforoListosSuspendidos.acquire();
                    try {
                        p.setState(ProcessState.SUSPENDED_READY);
                        Interfaz.colaListosSuspendidos.insert(p);
                        
                        // --- LOG --- (Reemplaza System.out)
                        Interfaz.logEvento("GestorIO: " + p.getName() + " (Disco) -> Listos/Susp (Disco).");
                        
                    } finally {
                        Interfaz.semaforoListosSuspendidos.release();
                    }
                    
                    // 6c. ¡¡NO SE HACE NOTIFY!! El proceso sigue en disco.
                }

            } catch (InterruptedException e) {
                simulacionActiva = false;
                
                // --- LOG --- (Reemplaza System.out)
                Interfaz.logEvento("GestorIO: Hilo interrumpido. Deteniendo.");
                
            } catch (Exception e) {
                
                // --- LOG --- (Reemplaza System.err)
                Interfaz.logEvento("GestorIO: ¡ERROR INESPERADO! " + e.getMessage());
                e.printStackTrace();
            }
        } // Fin del while (simulacionActiva)
        
        // --- LOG ---
        Interfaz.logEvento("GestorIO: Hilo detenido.");
    }
    
    /**
     * Función de ayuda para iterar una cola, restar tiempo
     * y marcar los procesos terminados en una lista.
     * (Esta función se ejecuta DENTRO de un semáforo)
     */
    private void marcarProcesosTerminados(Cola<Process> cola, ListaSimple<Process> listaMarcados) {
        Nodo<Process> actual = cola.getpFirst();
        while (actual != null) {
            Process p = actual.getData();
            p.setTiempoBloqueadoRestante(p.getTiempoBloqueadoRestante() - 1);
            
            if (p.getTiempoBloqueadoRestante() <= 0) {
                // --- LOG ---
                // (Logueamos cuando la I/O se completa)
                Interfaz.logEvento("GestorIO: I/O de " + p.getName() + " completada. Marcado para mover.");
                
                listaMarcados.addAtTheEnd(p); // Lo "marca"
            }
            actual = actual.getPnext();
        }
    }
    
    public void detener() {
        this.simulacionActiva = false;
    }
}