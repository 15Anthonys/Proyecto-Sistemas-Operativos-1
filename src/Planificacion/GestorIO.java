//

package Planificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ListaSimple; // Sigue usando tu ListaSimple
import EstructurasDeDatos.Nodo;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import soplanificacion.Interfaz;

/**
 * Hilo independiente que gestiona la cola de Bloqueados.
 * (VERSIÓN CORREGIDA SIN DEADLOCK)
 */
public class GestorIO implements Runnable {
    
    private boolean simulacionActiva = true;
    
    // Lista temporal SÓLO para "marcar" procesos.
    // No necesita semáforo porque solo este hilo la toca.
    private ListaSimple<Process> procesosListosParaSalir;

    public GestorIO() {
        this.procesosListosParaSalir = new ListaSimple<>();
    }
    
    @Override
    public void run() {
        while (simulacionActiva) {
            try {
                // 1. Duerme un "ciclo" de E/S
                Thread.sleep(1000); // Representa 1 ciclo de tiempo
                
                // --- LÓGICA DE DESBLOQUEO (Parte 1: Marcar) ---
                // Revisa la cola de bloqueados y "marca" los que terminaron.
                
                // 2. Pide el candado de Bloqueados
                Interfaz.semaforoBloqueados.acquire();
                try {
                    Nodo<Process> actual = Interfaz.colaBloqueados.getpFirst();
                    while (actual != null) {
                        Process p = actual.getData();
                        p.setTiempoBloqueadoRestante(p.getTiempoBloqueadoRestante() - 1);
                        
                        if (p.getTiempoBloqueadoRestante() <= 0) {
                            // 3. Lo "marca" para moverlo (lo añade a la lista local)
                            procesosListosParaSalir.addAtTheEnd(p);
                        }
                        actual = actual.getPnext();
                    }
                } finally {
                    // 4. Suelta el candado
                    Interfaz.semaforoBloqueados.release();
                }
                
                // --- LÓGICA DE MOVIMIENTO (Parte 2: Mover) ---
                // Mueve los procesos "marcados" uno por uno.
                
                if (procesosListosParaSalir.isEmpty()) {
                    continue; // No hay nada que mover, vuelve al inicio del bucle
                }

                // Itera sobre la lista de marcados
                while (!procesosListosParaSalir.isEmpty()) {
                    // Saca el primer proceso de la lista temporal
                    Process p = procesosListosParaSalir.getValueByIndex(0);
                    procesosListosParaSalir.deleteFirst();

                    // 5. Pide candado de Bloqueados (para SACAR)
                    Interfaz.semaforoBloqueados.acquire();
                    try {
                        Interfaz.colaBloqueados.remove(p); // Lo saca de bloqueados
                    } finally {
                        Interfaz.semaforoBloqueados.release();
                    }
                    
                    // 6. Pide candado de Listos (para METER)
                    Interfaz.semaforoListos.acquire();
                    try {
                        p.setState(ProcessState.READY);
                        Interfaz.colaListos.insert(p); // Lo mete en listos
                        System.out.println("GESTOR E/S: Proceso " + p.getName() + " DESBLOQUEADO -> Listos");
                    } finally {
                        Interfaz.semaforoListos.release();
                    }
                } // Fin del while (mover)

            } catch (InterruptedException e) {
                simulacionActiva = false;
                System.out.println("Gestor de E/S interrumpido.");
            } catch (Exception e) {
                System.err.println("Error en GestorIO: " + e.getMessage());
                e.printStackTrace();
            }
        } // Fin del while (simulacionActiva)
    }
    
    public void detener() {
        this.simulacionActiva = false;
    }
}