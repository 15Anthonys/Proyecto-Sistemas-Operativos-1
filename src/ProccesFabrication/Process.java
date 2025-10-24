package ProccesFabrication;

import soplanificacion.Interfaz; // Para acceder a semáforos, colas y contador global
import java.util.concurrent.Semaphore;

/**
 * Representa un Proceso del sistema operativo simulado.
 * Contiene su estado, contadores y la lógica de ejecución (método run).
 * (Versión corregida para FCFS/RR y manejo de errores/interrupciones)
 *
 * @author dugla (¡y Gemini!)
 */
public class Process implements Runnable {

    // --- Atributos del Proceso ---
    private String name;
    private int id;
    private volatile ProcessState state; // volatile para visibilidad entre hilos
    private int totalInstructions;
    private int programCounter;
    private int memoryAddressRegister; // Simulado
    private boolean isIOBound;
    private int ioExceptionCycle;    // Ciclo en que ocurre la E/S (si > 0)
    private int ioCompletionCycle;   // Duración de la E/S
    private long creationTime;
    private long completionTime;

    // --- Atributos para Planificación ---
    private volatile int tiempoBloqueadoRestante; // volatile
    private volatile int quantumRestante;         // volatile, -1 para "infinito"
    private int ciclosEnEspera;          // Para HRRN (no necesita ser volatile si solo lo toca el planificador)

    private static int nextId = 0; // Generador de IDs

    /**
     * Constructor.
     */
    public Process(String name, int totalInstructions, boolean isIOBound, int ioExceptionCycle, int ioCompletionCycle) {
        this.id = nextId++;
        this.name = name;
        this.totalInstructions = totalInstructions;
        this.isIOBound = isIOBound;

        // Asigna valores de E/S solo si es I/O Bound y los ciclos son válidos
        if (isIOBound && ioExceptionCycle > 0 && ioCompletionCycle > 0) {
            this.ioExceptionCycle = ioExceptionCycle;
            this.ioCompletionCycle = ioCompletionCycle;
        } else {
            this.isIOBound = false; // Asegura que sea CPU-Bound si los datos de E/S no son válidos
            this.ioExceptionCycle = -1;
            this.ioCompletionCycle = -1;
        }

        this.state = ProcessState.NEW; // Estado inicial
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        this.creationTime = System.currentTimeMillis();
        this.completionTime = -1;

        // Inicializa valores de planificación
        this.tiempoBloqueadoRestante = 0;
        this.quantumRestante = 0; // El Planificador lo asignará
        this.ciclosEnEspera = 0;
    }


    /**
     * Lógica principal del hilo del proceso. Compite por la CPU y ejecuta ciclos.
     */
    @Override
    public void run() {
        try {
            // 1. Espera inicial (a ser despertado por el Planificador la primera vez)
            synchronized (this) {
                // System.out.println("Hilo " + name + " esperando para iniciar..."); // Log opcional
                this.wait();
            }
            // System.out.println("Hilo " + name + " iniciado!"); // Log opcional

            // Bucle principal de vida del proceso
            while (this.state != ProcessState.TERMINATED && !Thread.currentThread().isInterrupted()) {

                // 2. Adquirir la CPU (bloquea si está ocupada)
                // System.out.println("Proceso " + name + " intentando adquirir CPU..."); // Log opcional
                Interfaz.semaforoCPU.acquire();
                // System.out.println("Proceso " + name + " obtuvo CPU."); // Log opcional

                // Resetear estado al inicio de la ráfaga
                ProcessState proximoEstadoDespuesDeCPU = ProcessState.RUNNING;
                boolean ejecutoAlMenosUnCiclo = false;

                try {
                    // --- SECCIÓN CRÍTICA: EJECUCIÓN EN CPU ---
                    this.state = ProcessState.RUNNING; // Marcar como ejecutando
                    Interfaz.procesoEnCPU = this;       // Informar a la GUI
                    System.out.println("¡Proceso " + name + " ESTÁ EN LA CPU!");

                    // Bucle de ejecución: mientras tenga permiso (Q > 0 o Q == -1) Y no deba salir
                    while ((this.quantumRestante == -1 || this.quantumRestante > 0)
                           && proximoEstadoDespuesDeCPU == ProcessState.RUNNING
                           && !Thread.currentThread().isInterrupted()) { // Chequeo extra de interrupción

                        ejecutoAlMenosUnCiclo = true; // Marcamos que se ejecutó

                        // Simular un ciclo de CPU
                        this.programCounter++;
                        this.memoryAddressRegister++; // Simulación simple de MAR

                        // Decrementar quantum solo si es finito (no -1)
                        if (this.quantumRestante != -1) {
                            this.quantumRestante--;
                        }

                        // Imprimir estado actual
                        System.out.println("  " + name + " (RUN): PC=" + getProgramCounter()
                                + ", Q=" + (this.quantumRestante == -1 ? "inf" : this.quantumRestante));

                        // Simular tiempo de trabajo del ciclo
                        Thread.sleep(700); // <-- AJUSTA ESTE VALOR PARA LA VELOCIDAD

                        // Verificar si debe salir de la CPU
                        if (this.programCounter >= this.totalInstructions) {
                            proximoEstadoDespuesDeCPU = ProcessState.TERMINATED; // Terminar
                        } else if (this.isIOBound && (this.programCounter % this.ioExceptionCycle == 0)) {
                            // Si es I/O Bound y llegó al ciclo de excepción
                            this.tiempoBloqueadoRestante = this.ioCompletionCycle; // Configura duración E/S
                            proximoEstadoDespuesDeCPU = ProcessState.BLOCKED; // Bloquear
                        }
                        // Si quantumRestante llegó a 0 (y no era -1), el bucle terminará naturalmente
                    } // --- Fin del bucle while de ejecución ---

                    // Determinar el estado final después de salir del bucle
                    if (proximoEstadoDespuesDeCPU == ProcessState.RUNNING) {
                        // Si salió del bucle y sigue en RUNNING, fue por quantum (o error)
                        if (this.quantumRestante == 0 && ejecutoAlMenosUnCiclo) {
                             proximoEstadoDespuesDeCPU = ProcessState.READY; // Salió por Quantum (RR)
                        } else if (this.quantumRestante == -1 && ejecutoAlMenosUnCiclo) {
                             // Salió con Q=-1 sin terminar/bloquear (posible interrupción externa?)
                             System.err.println("ADVERTENCIA: " + name + " salió de CPU con Q=-1 sin terminar/bloquear.");
                             proximoEstadoDespuesDeCPU = ProcessState.READY; // Mandar a Ready por seguridad
                        } else if (!ejecutoAlMenosUnCiclo) {
                             // ¡BUG! Entró a CPU pero no ejecutó (Q era 0 al inicio?)
                             System.err.println("ERROR LÓGICO: " + name + " en CPU pero no ejecutó ciclos. Q=" + this.quantumRestante);
                             // Intenta una transición segura: si es RR va a Ready, si es FCFS debería terminar o bloquear
                             proximoEstadoDespuesDeCPU = ProcessState.READY; // O decide basado en el algoritmo
                        }
                    }
                    // Si proximoEstadoDespuesDeCPU ya era TERMINATED o BLOCKED, se mantiene.

                } finally {
                    // --- LIBERAR LA CPU (SIEMPRE) ---
                    System.out.println("Proceso " + name + " LIBERA LA CPU.");
                    Interfaz.procesoEnCPU = null;       // Informar a la GUI que CPU está libre
                    Interfaz.semaforoCPU.release();     // Liberar el semáforo de la CPU
                    // --- FIN LIBERAR CPU ---
                }
                // --- FIN SECCIÓN CRÍTICA ---


                // --- 5. TRANSICIÓN DE ESTADO (Fuera de la CPU) ---
                // Mover el proceso a la cola correspondiente y esperar si es necesario

                if (proximoEstadoDespuesDeCPU == ProcessState.TERMINATED) {
                    this.state = ProcessState.TERMINATED;
                    this.completionTime = System.currentTimeMillis(); // Registrar tiempo final
                    System.out.println("Proceso " + name + " -> TERMINADO.");
                    Interfaz.contadorProcesosEnMemoria.decrementAndGet(); // Avisa al PMP
                    // Añadir a cola Terminados (protegido)
                    Interfaz.semaforoTerminados.acquire();
                    try { Interfaz.colaTerminados.insert(this); }
                    finally { Interfaz.semaforoTerminados.release(); }
                    break; // Termina el bucle while principal y el hilo muere

                } else if (proximoEstadoDespuesDeCPU == ProcessState.BLOCKED) {
                    this.state = ProcessState.BLOCKED;
                    System.out.println("Proceso " + name + " -> BLOQUEADO (esperando E/S por " + this.tiempoBloqueadoRestante + " ciclos).");
                    // Añadir a cola Bloqueados (protegido)
                    Interfaz.semaforoBloqueados.acquire();
                    try { Interfaz.colaBloqueados.insert(this); }
                    finally { Interfaz.semaforoBloqueados.release(); }
                    // Espera a ser despertado por GestorIO
                    synchronized (this) { this.wait(); }

                } else if (proximoEstadoDespuesDeCPU == ProcessState.READY) {
                    this.state = ProcessState.READY;
                    // Determina el motivo para el mensaje
                    String motivo = (this.quantumRestante == 0 && ejecutoAlMenosUnCiclo) ? "(por Quantum)" : "(preempted/otro)";
                    System.out.println("Proceso " + name + " -> LISTOS " + motivo);
                    // Añadir a cola Listos (protegido)
                    Interfaz.semaforoListos.acquire();
                    try { Interfaz.colaListos.insert(this); }
                    finally { Interfaz.semaforoListos.release(); }
                    // Espera a ser despertado por el Planificador
                    synchronized (this) { this.wait(); }
                } else {
                    // Estado inesperado, podría ser un error
                     System.err.println("ERROR: Estado inesperado " + proximoEstadoDespuesDeCPU + " para proceso " + name + " después de CPU.");
                     this.state = ProcessState.TERMINATED; // Terminar por seguridad
                     Interfaz.contadorProcesosEnMemoria.decrementAndGet();
                     break; // Salir del bucle
                }
            } // --- Fin del bucle while principal ---

        } catch (InterruptedException e) {
            // Manejo de interrupción externa del hilo (ej. al detener simulación)
            System.out.println("Proceso " + name + " interrumpido externamente.");
            // Asegurarse de decrementar contador si no llegó a TERMINATED normalmente
            if (this.state != ProcessState.TERMINATED) {
                Interfaz.contadorProcesosEnMemoria.decrementAndGet();
            }
            this.state = ProcessState.TERMINATED; // Marcar como terminado
            Thread.currentThread().interrupt(); // Restablecer flag de interrupción

        } catch (Exception e) {
            // Capturar cualquier otro error inesperado durante la ejecución
             System.err.println("ERROR CRÍTICO en hilo " + name + ": " + e.getMessage());
             e.printStackTrace();
             // Intentar limpiar estado y memoria
             if (this.state != ProcessState.TERMINATED) {
                 Interfaz.contadorProcesosEnMemoria.decrementAndGet();
             }
             this.state = ProcessState.TERMINATED;

        } finally {
             // Bloque finally se ejecuta siempre, incluso si hay error o interrupción
             // Asegura liberar la CPU si el hilo muere inesperadamente mientras la tenía
             if (Interfaz.procesoEnCPU == this) { // Solo si ESTE proceso tenía la CPU
                 try {
                     Interfaz.semaforoCPU.release();
                     Interfaz.procesoEnCPU = null;
                     System.err.println("ADVERTENCIA: Hilo " + name + " muerto o interrumpido, liberando CPU forzosamente.");
                 } catch (Exception ex) {
                     // Podría dar error si ya se liberó, ignorar.
                 }
             }
             System.out.println("--- Hilo de " + name + " ha muerto. ---");
        }
    } // --- Fin del método run() ---


    // --- GETTERS Y SETTERS ---
    // (Asegúrate de tenerlos todos, especialmente los nuevos para planificación)

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; } // Podría necesitar sincronización si otros hilos lo modifican
    public int getTotalInstructions() { return totalInstructions; }
    public void setTotalInstructions(int totalInstructions) { this.totalInstructions = totalInstructions; }
    public int getProgramCounter() { return programCounter; }
    public void setProgramCounter(int programCounter) { this.programCounter = programCounter; }
    public int getMemoryAddressRegister() { return memoryAddressRegister; }
    public void setMemoryAddressRegister(int memoryAddressRegister) { this.memoryAddressRegister = memoryAddressRegister; }
    public boolean isIsIOBound() { return isIOBound; }
    public void setIsIOBound(boolean isIOBound) { this.isIOBound = isIOBound; }
    public int getIoExceptionCycle() { return ioExceptionCycle; }
    public void setIoExceptionCycle(int ioExceptionCycle) { this.ioExceptionCycle = ioExceptionCycle; }
    public int getIoCompletionCycle() { return ioCompletionCycle; }
    public void setIoCompletionCycle(int ioCompletionCycle) { this.ioCompletionCycle = ioCompletionCycle; }
    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }
    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }

    // Getters y Setters para atributos de planificación
    public int getTiempoBloqueadoRestante() { return tiempoBloqueadoRestante; }
    public void setTiempoBloqueadoRestante(int t) { this.tiempoBloqueadoRestante = t; } // Podría necesitar sincronización
    public int getQuantumRestante() { return quantumRestante; }
    public void setQuantumRestante(int q) { this.quantumRestante = q; } // Podría necesitar sincronización
    public int getCiclosEnEspera() { return ciclosEnEspera; }
    public void setCiclosEnEspera(int c) { this.ciclosEnEspera = c; }

    /** Devuelve las instrucciones restantes (útil para SRT). */
    public int getInstruccionesRestantes() {
        return Math.max(0, this.totalInstructions - this.programCounter); // Asegura no ser negativo
    }

    // Método toString (opcional, para debugging)
    @Override
    public String toString() {
        return "Process{" + "name=" + name + ", id=" + id + ", state=" + state + ", PC=" + programCounter + '}';
    }
}