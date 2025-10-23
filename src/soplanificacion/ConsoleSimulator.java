package soplanificacion; // Or your main package

// --- Import necessary classes ---
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import Planificacion.*; // Import algorithms, Planificador, GestorIO
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 * Simulador de consola para probar la lógica concurrente
 * (Hilos, Semáforos, Algoritmos) sin la GUI.
 */
public class ConsoleSimulator {

    // --- Referencias a los recursos compartidos de Interfaz ---
    // Make sure these are declared as 'public static' in Interfaz.java
    static Cola<Process> colaNuevos = new Cola<>(); // This one is local for setup
    static Cola<Process> colaListos = Interfaz.colaListos;
    static Cola<Process> colaBloqueados = Interfaz.colaBloqueados;
    static Cola<Process> colaTerminados = Interfaz.colaTerminados;
    static Semaphore semaforoListos = Interfaz.semaforoListos;
    static Semaphore semaforoBloqueados = Interfaz.semaforoBloqueados;
    static Semaphore semaforoTerminados = Interfaz.semaforoTerminados;

    // --- Threads ---
    static Planificador planificador;
    static GestorIO gestorIO;
    static Thread hiloPlanificador;
    static Thread hiloGestorIO;
    static ArrayList<Thread> hilosProcesos = new ArrayList<>(); // To store process threads

    public static void main(String[] args) {

        System.out.println("--- INICIANDO SIMULADOR DE CONSOLA ---");

        // --- 1. CONFIGURACIÓN ---
        // Choose the algorithm to test:
       //SchedulerAlgorithm algoritmo = new FCFSAlgorithm();
         SchedulerAlgorithm algoritmo = new RoundRobinAlgorithm(3); // RR with Quantum=3
        // SchedulerAlgorithm algoritmo = new SPNAlgorithm();
        // SchedulerAlgorithm algoritmo = new SRTAlgorithm(); // Requires careful implementation for preemption
        // SchedulerAlgorithm algoritmo = new HRRNAlgorithm();

        System.out.println("Usando Algoritmo: " + algoritmo.getClass().getSimpleName());

        // Create test processes and add them to the local 'colaNuevos'
        crearProcesosDePrueba();

        // --- 2. START SIMULATION (Like clicking the "Iniciar" button) ---
        iniciarSimulacion(algoritmo);

        // --- 3. WAIT FOR SIMULATION TO END ---
        // Wait until all created processes are in the 'colaTerminados'
        esperarFinSimulacion(hilosProcesos.size());

        // --- 4. STOP HELPER THREADS ---
        detenerSimulacion();

        // --- 5. SHOW FINAL RESULTS ---
        System.out.println("\n--- RESULTADOS FINALES ---");
        System.out.print("Terminados (" + colaTerminados.getSize() + "): ");
        imprimirCola(colaTerminados, semaforoTerminados); // Use the printing helper

        System.out.println("\n--- SIMULADOR DE CONSOLA FINALIZADO ---");
        System.exit(0); // Force exit if some threads didn't terminate cleanly
    }

    // --- Helper Methods ---

    /**
     * Creates example processes and adds them to the local 'colaNuevos'.
     */
    static void crearProcesosDePrueba() {
        System.out.println("Creando procesos de prueba...");
        // (Name, TotalInst, IsIOBound?, IO_ExceptionCycle, IO_CompletionCycle)
        colaNuevos.insert(new Process("A(CPU-L)", 10, false, -1, -1));
        colaNuevos.insert(new Process("B(CPU-C)", 3, false, -1, -1));
        colaNuevos.insert(new Process("C(I/O)", 8, true, 3, 5)); // Blocks at PC=3 & 6, takes 5 cycles
        colaNuevos.insert(new Process("D(CPU-C)", 4, false, -1, -1));
        System.out.println("Procesos creados ("+ colaNuevos.getSize() +") en 'colaNuevos' local.");
    }

    /**
     * Simulates the logic of BotonIniciarActionPerformed:
     * Moves processes from 'Nuevos' to 'Listos' and starts all threads.
     */
    static void iniciarSimulacion(SchedulerAlgorithm algoritmo) {
        System.out.println("Iniciando simulación...");

        // Move from Nuevos to Listos and start process threads
        // No semaphore needed here as other threads haven't started yet
        while (!colaNuevos.isEmpty()) {
            Process p = colaNuevos.pop();
            p.setState(ProcessState.READY); // Set initial state

            // Create and start the thread for this process
            Thread t = new Thread(p, "Hilo-" + p.getName());
            hilosProcesos.add(t); // Keep track of the thread
            t.start();

            // Add to the shared 'colaListos' (needs protection later, but ok here)
            try {
                 // **Acquire lock before modifying shared list**
                 semaforoListos.acquire();
                 try {
                      Interfaz.colaListos.insert(p);
                 } finally {
                      // **Release lock even if error occurs**
                      semaforoListos.release();
                 }
            } catch(InterruptedException e){
                 Thread.currentThread().interrupt(); // Restore interrupted status
                 System.err.println("Failed to add process "+ p.getName() +" to ready queue: "+ e.getMessage());
            }

        }
        System.out.println("Hilos de procesos iniciados: " + hilosProcesos.size());


        // Start the Planificador thread
        planificador = new Planificador(algoritmo);
        hiloPlanificador = new Thread(planificador, "Hilo-Planificador");
        hiloPlanificador.start();
        System.out.println("Hilo Planificador iniciado.");

        // Start the GestorIO thread
        gestorIO = new GestorIO();
        hiloGestorIO = new Thread(gestorIO, "Hilo-GestorIO");
        hiloGestorIO.start();
        System.out.println("Hilo GestorIO iniciado.");
        System.out.println("\n--- Simulación en curso... (Ctrl+C para detener si se cuelga) ---");
    }

    /**
     * Waits until the number of processes in the terminated queue
     * matches the expected total number of processes.
     */
    static void esperarFinSimulacion(int numeroTotalProcesos) {
        System.out.println("Esperando finalización de " + numeroTotalProcesos + " procesos...");
        int lastReportedSize = -1;
        while (true) {
            try {
                int currentSize = 0;
                // Acquire lock to safely check the size
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

                if (currentSize >= numeroTotalProcesos) {
                    break; // Exit loop if all processes have terminated
                }
                // Wait a bit before checking again
                Thread.sleep(500); // Check every 0.5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                System.err.println("Espera de finalización interrumpida.");
                break;
            }
        }
        System.out.println("Todos los procesos han terminado.");
    }

     /**
     * Signals the Planificador and GestorIO threads to stop their loops
     * and interrupts them in case they are sleeping.
     */
    static void detenerSimulacion() {
        System.out.println("Enviando señal de detención a hilos auxiliares...");

        // Signal threads to stop gracefully
        if (planificador != null) planificador.detener();
        if (gestorIO != null) gestorIO.detener();

        // Interrupt threads in case they are sleeping (e.g., in Thread.sleep or wait())
        if (hiloPlanificador != null) {
            hiloPlanificador.interrupt();
             System.out.println("Interrumpiendo Planificador...");
        }
        if (hiloGestorIO != null) {
            hiloGestorIO.interrupt();
             System.out.println("Interrumpiendo GestorIO...");
        }


        // Optional: Wait briefly for threads to actually finish
        try {
            System.out.println("Esperando que los hilos auxiliares terminen...");
            if (hiloPlanificador != null) hiloPlanificador.join(1000); // Wait max 1 sec
            if (hiloGestorIO != null) hiloGestorIO.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupción mientras se esperaba la finalización de hilos auxiliares.");
        }
         System.out.println("Hilos auxiliares deberían haberse detenido.");
    }

    /**
     * Safely prints the content of a shared queue using its semaphore.
     */
    static void imprimirCola(Cola<Process> cola, Semaphore sem) {
        StringBuilder sb = new StringBuilder("[ ");
        if (sem != null) {
            try {
                // Acquire the semaphore before accessing the queue
                sem.acquire();
                try {
                    Nodo<Process> actual = cola.getpFirst();
                    while (actual != null) {
                        sb.append(actual.getData().getName()).append(" ");
                        actual = actual.getPnext();
                    }
                } finally {
                    // Always release the semaphore
                    sem.release();
                }
            } catch (InterruptedException e) {
                 Thread.currentThread().interrupt(); // Restore interrupted status
                 sb.append(" ERROR AL LEER COLA ");
            }
        } else { // For queues not needing a semaphore (like the local colaNuevos)
             Nodo<Process> actual = cola.getpFirst();
             while (actual != null) {
                 sb.append(actual.getData().getName()).append(" ");
                 actual = actual.getPnext();
             }
        }
        sb.append("]");
        System.out.print(sb.toString()); // Use print instead of println for inline display
    }
}