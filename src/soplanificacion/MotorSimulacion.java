package soplanificacion;

import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import Planificacion.*;
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;

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
    static SimpleSemaphore semaforoNuevos = Interfaz.semaforoNuevos;
    static SimpleSemaphore semaforoListos = Interfaz.semaforoListos;
    static SimpleSemaphore semaforoBloqueados = Interfaz.semaforoBloqueados;
    static SimpleSemaphore semaforoTerminados = Interfaz.semaforoTerminados;

    // --- Threads ---
    public static Planificador planificador;
    static GestorIO gestorIO;
    static PlanificadorMedianoPlazo pmp;

    static Thread hiloPlanificador;
    static Thread hiloGestorIO;
    static Thread hiloPMP;

    public static Cola<Thread> hilosProcesos = new Cola<>();
    public static SimpleSemaphore semaforoHilosProcesos = new SimpleSemaphore(1);

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

        // Iniciar Planificador primero
        planificador = new Planificador(algoritmo);
        // Duración de ciclo desde config/slider
        int ms = ConfigIO.loadCycleMs();
        planificador.setCycleMs(ms);
        hiloPlanificador = new Thread(planificador, "Hilo-Planificador");
        hiloPlanificador.start();
        System.out.println("Hilo Planificador (Corto Plazo) iniciado.");

        // Mover procesos de Nuevos a RAM/READY y lanzar hilos de proceso
        while (!Interfaz.colaNuevos.isEmpty()) {
            Process p = null;
            try {
                Interfaz.semaforoNuevos.acquire();
                try { p = Interfaz.colaNuevos.pop(); }
                finally { Interfaz.semaforoNuevos.release(); }

                if (p == null) continue;

                p.setState(ProcessState.READY);
                p.arrivalCycle = Interfaz.globalClock.get();

                Thread t = new Thread(p, "Hilo-" + p.getName());
                // hilosProcesos.add(t); // eliminado
                hilosProcesos.insert(t);
                t.start();

                Interfaz.contadorProcesosEnMemoria.incrementAndGet();
                planificador.admitirAListos(p, Interfaz.globalClock.get());
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                System.err.println("Error al admitir proceso a READY.");
            }
        }
        System.out.println("Procesos cargados en RAM y encolados a READY.");

        // GestorIO
        gestorIO = new GestorIO();
        hiloGestorIO = new Thread(gestorIO, "Hilo-GestorIO");
        hiloGestorIO.start();
        System.out.println("Hilo GestorIO iniciado.");

        // PMP
        pmp = new PlanificadorMedianoPlazo();
        hiloPMP = new Thread(pmp, "Hilo-PMP");
        hiloPMP.start();
        System.out.println("Hilo Planificador de Mediano Plazo iniciado.");

        System.out.println("\n--- Simulación en curso ---");
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
    static void imprimirCola(Cola<Process> cola, SimpleSemaphore sem) {
        StringBuilder sb = new StringBuilder("[ ");
        if (sem != null) {
            try {
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