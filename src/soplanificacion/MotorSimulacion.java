package soplanificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import Planificacion.*;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Simulador de consola que implementa un Planificador de Mediano Plazo (PMP)
 * para el control de admisión y suspensión.
 * (VERSIÓN CORREGIDA)
 */
public class MotorSimulacion {

    // --- Referencias a las colas GLOBALES (de Interfaz) ---
    static Cola<Process> colaNuevos = Interfaz.colaNuevos;
    static Cola<Process> colaListos = Interfaz.colaListos;
    static Cola<Process> colaBloqueados = Interfaz.colaBloqueados;
    static Cola<Process> colaTerminados = Interfaz.colaTerminados;
    
    // --- Referencias a los semáforos GLOBALES (de Interfaz) ---
    static Semaphore semaforoNuevos = Interfaz.semaforoNuevos;
    static Semaphore semaforoListos = Interfaz.semaforoListos;
    static Semaphore semaforoBloqueados = Interfaz.semaforoBloqueados;
    static Semaphore semaforoTerminados = Interfaz.semaforoTerminados;

    // --- Threads ---
    static Planificador planificador;
    static GestorIO gestorIO;
    static PlanificadorMedianoPlazo pmp;
    
    static Thread hiloPlanificador;
    static Thread hiloGestorIO;
    static Thread hiloPMP;
    
    // Esta lista AHORA la llenará el PMP
    public static ArrayList<Thread> hilosProcesos = new ArrayList<>();
    
    // Semáforo para proteger la lista de hilos
    public static Semaphore semaforoHilosProcesos = new Semaphore(1);

    public static void main(String[] args) {

        System.out.println("--- INICIANDO SIMULADOR DE CONSOLA ---");
        SchedulerAlgorithm algoritmo = new RoundRobinAlgorithm(3); // RR con Quantum=3
        System.out.println("Usando Algoritmo: " + algoritmo.getClass().getSimpleName());

        // Guardamos el total ANTES de que el PMP empiece a sacarlos
        int totalProcesosCreados = crearProcesosDePrueba();

        // Inicia los hilos auxiliares (Planificadores, GestorIO)
        iniciarSimulacion(algoritmo);
        
        // Espera a que el número de terminados coincida con los creados
        esperarFinSimulacion(totalProcesosCreados); 

        // Detiene todos los hilos
        detenerSimulacion();

        // Muestra resultados finales
        System.out.println("\n--- RESULTADOS FINALES ---");
        System.out.print("Terminados (" + colaTerminados.getSize() + "): ");
        imprimirCola(colaTerminados, semaforoTerminados);

        System.out.println("\n--- SIMULADOR DE CONSOLA FINALIZADO ---");
        System.exit(0);
    }

    /**
     * Crea los procesos de prueba y los añade a la cola global de Nuevos.
     * Devuelve el número total de procesos creados.
     */
    static int crearProcesosDePrueba() {
        System.out.println("Creando procesos de prueba...");
        int total = 0;
        try {
            // Pide el semáforo de la colaNuevos para modificarla
            semaforoNuevos.acquire();
            try {
                colaNuevos.insert(new Process("A(CPU-L)", 10, false, -1, -1));
                colaNuevos.insert(new Process("B(CPU-C)", 3, false, -1, -1));
                colaNuevos.insert(new Process("C(I/O)", 8, true, 3, 5));
                colaNuevos.insert(new Process("D(CPU-C)", 4, false, -1, -1));
                colaNuevos.insert(new Process("E(CPU-L)", 10, false, -1, -1));
                colaNuevos.insert(new Process("F(I/O)", 9, true, 2, 4));
                
                total = colaNuevos.getSize();
            } finally {
                semaforoNuevos.release();
            }
        } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             System.err.println("Error al crear procesos: " + e.getMessage());
        }
        
        System.out.println("Procesos creados ("+ total +") en 'colaNuevos' global.");
        return total;
    }

    /**
     * Inicia los hilos de fondo (Planificadores y GestorIO).
     * YA NO inicia los hilos de los procesos.
     */
    static void iniciarSimulacion(SchedulerAlgorithm algoritmo) {
        System.out.println("Iniciando simulación...");

        // <<< ¡CAMBIO! --- Volvemos a poner el bucle ---
        while (!Interfaz.colaNuevos.isEmpty()) {
            Process p = null;
            try {
                // Saca de Nuevos
                Interfaz.semaforoNuevos.acquire();
                try {
                    p = Interfaz.colaNuevos.pop();
                } finally {
                    Interfaz.semaforoNuevos.release();
                }
                
                if (p == null) continue;

                p.setState(ProcessState.READY); 
                
                // Inicia el Hilo
                Thread t = new Thread(p, "Hilo-" + p.getName());
                hilosProcesos.add(t); // (Protección de semáforo omitida por simplicidad, está bien aquí)
                t.start();

                // Mete en Listos (RAM)
                Interfaz.semaforoListos.acquire();
                try {
                    Interfaz.colaListos.insert(p);
                } finally {
                    Interfaz.semaforoListos.release();
                }
                
                // <<< ¡MUY IMPORTANTE! --- Incrementa el contador 6 veces ---
                Interfaz.contadorProcesosEnMemoria.incrementAndGet();
                System.out.println("...Cargando " + p.getName() + " en RAM. (Total en RAM: " + Interfaz.contadorProcesosEnMemoria.get() + ")");

            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.err.println("Failed to add process "+ (p != null ? p.getName() : "") +" to ready queue: "+ e.getMessage());
            }
        }
        System.out.println("¡TODOS LOS 6 PROCESOS CARGADOS EN RAM!");

        // Start the Planificador (Corto Plazo)
        planificador = new Planificador(algoritmo);
        hiloPlanificador = new Thread(planificador, "Hilo-Planificador");
        hiloPlanificador.start();
        System.out.println("Hilo Planificador (Corto Plazo) iniciado.");

        // Start the GestorIO
        gestorIO = new GestorIO();
        hiloGestorIO = new Thread(gestorIO, "Hilo-GestorIO");
        hiloGestorIO.start();
        System.out.println("Hilo GestorIO iniciado.");
        
        // Start the PlanificadorMedianoPlazo
        pmp = new PlanificadorMedianoPlazo();
        hiloPMP = new Thread(pmp, "Hilo-PMP");
        hiloPMP.start();
        System.out.println("Hilo Planificador de Mediano Plazo iniciado.");

        System.out.println("\n--- Simulación en curso... (Ctrl+C para detener si se cuelga) ---");
    }

    /**
     * Bucle de espera que revisa la cola de Terminados
     * hasta que su tamaño iguala al total de procesos creados.
     */
    static void esperarFinSimulacion(int numeroTotalProcesos) {
        System.out.println("Esperando finalización de " + numeroTotalProcesos + " procesos...");
        int lastReportedSize = -1;
        while (true) {
            try {
                int currentSize = 0;
                // Pide semáforo para leer el tamaño de la cola
                semaforoTerminados.acquire();
                try {
                    currentSize = colaTerminados.getSize();
                } finally {
                    semaforoTerminados.release();
                }

                if (currentSize != lastReportedSize) {
                   // System.out.println("... procesos terminados: " + currentSize + "/" + numeroTotalProcesos);
                    lastReportedSize = currentSize;
                }

                // Condición de salida
                if (currentSize >= numeroTotalProcesos) {
                    break; 
                }
                
                // Espera un poco antes de volver a revisar
                Thread.sleep(500);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Espera de finalización interrumpida.");
                break;
            }
        }
        System.out.println("Todos los procesos han terminado.");
    }

    /**
     * Llama a detener() e interrumpe todos los hilos auxiliares.
     */
    static void detenerSimulacion() {
        System.out.println("Enviando señal de detención a hilos auxiliares...");

        // Envía señal de parada (flag booleano)
        if (planificador != null) planificador.detener();
        if (gestorIO != null) gestorIO.detener();
        if (pmp != null) pmp.detener(); 

        // Interrumpe los hilos (por si están en sleep o wait)
        if (hiloPlanificador != null) {
            hiloPlanificador.interrupt();
             System.out.println("Interrumpiendo Planificador...");
        }
        if (hiloGestorIO != null) {
            hiloGestorIO.interrupt();
             System.out.println("Interrumpiendo GestorIO...");
        }
        if (hiloPMP != null) { 
            hiloPMP.interrupt();
             System.out.println("Interrumpiendo PMP...");
        }

        // Espera (join) a que los hilos mueran
        try {
            System.out.println("Esperando que los hilos auxiliares terminen...");
            if (hiloPlanificador != null) hiloPlanificador.join(1000);
            if (hiloGestorIO != null) hiloGestorIO.join(1000);
            if (hiloPMP != null) hiloPMP.join(1000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupción mientras se esperaba la finalización de hilos auxiliares.");
        }
         System.out.println("Hilos auxiliares deberían haberse detenido.");
    }

    /**
     * Imprime de forma segura el contenido de una cola.
     */
    static void imprimirCola(Cola<Process> cola, Semaphore sem) {
        StringBuilder sb = new StringBuilder("[ ");
        if (sem != null) {
            try {
                // Pide semáforo para leer la cola
                sem.acquire();
                try {
                    Nodo<Process> actual = cola.getpFirst();
                    while (actual != null) {
                        sb.append(actual.getData().getName()).append(" ");
                        actual = actual.getPnext();
                    }
                } finally {
                    sem.release();
                }
            } catch (InterruptedException e) {
                 Thread.currentThread().interrupt();
                 sb.append(" ERROR AL LEER COLA ");
            }
        } else {
             // Para colas no compartidas (aunque ya no deberíamos tener)
             Nodo<Process> actual = cola.getpFirst();
             while (actual != null) {
                 sb.append(actual.getData().getName()).append(" ");
                 actual = actual.getPnext();
             }
        }
        sb.append("]");
        System.out.print(sb.toString());
    }
}