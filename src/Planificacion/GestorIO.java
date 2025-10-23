package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz;

/**
 * Hilo que gestiona AMBAS colas de Bloqueados (RAM y Disco).
 * (VERSIÓN ACTUALIZADA PARA ESTADOS SUSPENDIDOS)
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
                Thread.sleep(1000); // Representa 1 ciclo de tiempo
                
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
                        System.out.println("GESTOR E/S: Proceso " + p.getName() + " (RAM) -> Listos (RAM)");
                    } finally {
                        Interfaz.semaforoListos.release();
                    }

                    // 5c. ¡¡DESPIERTA AL HILO!! (Tu Process.java lo necesita)
                    // (Esto arregla un bug que tenías)
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
                        p.setState(ProcessState.SUSPENDED_READY); // Tu Enum usa este nombre
                        Interfaz.colaListosSuspendidos.insert(p);
                        System.out.println("GESTOR E/S: Proceso " + p.getName() + " (Disco) -> Listos/Susp (Disco)");
                    } finally {
                        Interfaz.semaforoListosSuspendidos.release();
                    }
                    
                    // 6c. ¡¡NO SE HACE NOTIFY!! El proceso sigue en disco.
                    // El PMP se encargará de él.
                }

            } catch (InterruptedException e) {
                simulacionActiva = false;
                System.out.println("Gestor de E/S interrumpido.");
            } catch (Exception e) {
                System.err.println("Error en GestorIO: " + e.getMessage());
                e.printStackTrace();
            }
        } // Fin del while (simulacionActiva)
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
                listaMarcados.addAtTheEnd(p); // Lo "marca"
            }
            actual = actual.getPnext();
        }
    }
    
    public void detener() {
        this.simulacionActiva = false;
    }
}