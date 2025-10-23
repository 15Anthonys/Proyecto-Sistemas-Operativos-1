package ProccesFabrication;

import java.util.concurrent.atomic.AtomicInteger;
import soplanificacion.Interfaz; // Para acceder a los semáforos y colas
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author dugla
 * (MODIFICADO PARA SIMULACIÓN CONCURRENTE)
 */
public class Process implements Runnable {
    
    
    private String name;
    private int id;
    private ProcessState state;
    private int totalInstructions;
    private int programCounter;
    private int memoryAddressRegister;
    private boolean isIOBound;
    private int ioExceptionCycle;
    private int ioCompletionCycle;
    private long creationTime;
    private long completionTime;
    
    // --- ATRIBUTOS AÑADIDOS PARA PLANIFICACIÓN ---
    /** Cuántos ciclos de E/S le quedan (para la cola de Bloqueados). */
    private int tiempoBloqueadoRestante;
    
    /** Cuántos ciclos de CPU le quedan en este turno (para Round Robin). */
    private int quantumRestante;
    
    /** Cuántos ciclos ha pasado en la cola de Listos (para HRRN). */
    private int ciclosEnEspera;
    
    
    private static int nextId = 0; // Variable estática para generar IDs únicos

    /**
    * Constructor para crear un nuevo proceso con todos sus datos iniciales.
    */
    public Process(String name, int totalInstructions, boolean isIOBound, int ioExceptionCycle, int ioCompletionCycle) {
        this.id = nextId++; // Asigna un ID único y lo incrementa para el siguiente
        this.name = name;
        this.totalInstructions = totalInstructions;
        this.isIOBound = isIOBound;

        if (isIOBound) {
            this.ioExceptionCycle = ioExceptionCycle;
            this.ioCompletionCycle = ioCompletionCycle;
        } else {
            this.ioExceptionCycle = -1;
            this.ioCompletionCycle = -1;
        }

        this.state = ProcessState.NEW; 
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        this.creationTime = System.currentTimeMillis(); 
        this.completionTime = -1; 
        
        // Inicializamos los nuevos valores
        this.tiempoBloqueadoRestante = 0;
        this.quantumRestante = 0; // El planificador lo asignará
        this.ciclosEnEspera = 0;
    }

    
    /**
     * ¡EL MÉTODO RUN! Esta es la "vida" del proceso.
     * Contiene la lógica para competir por la CPU usando semáforos.
     */
    @Override
    public void run() {
    try {
        // 1. El hilo empieza y se duerme inmediatamente.
        // Esperará a que el Planificador lo despierte por primera vez.
        synchronized (this) {
            this.wait(); 
        }

        // Bucle de vida del proceso
        while (this.state != ProcessState.TERMINATED) {
            
            // 2. ADQUIRIR CPU (El Planificador ya nos despertó)
            // El Planificador nos dio "permiso", ahora tomamos la CPU físicamente.
            Interfaz.semaforoCPU.acquire(); 
            
            // --- 3. SECCIÓN CRÍTICA (TENGO LA CPU) ---
            ProcessState proximoEstado = ProcessState.RUNNING; // Estado por defecto
            try {
                System.out.println("¡Proceso " + name + " ESTÁ EN LA CPU!");
                this.setState(ProcessState.RUNNING);

                // Bucle de ráfaga de CPU (mientras tenga quantum y no deba salir)
                while (this.quantumRestante > 0 && proximoEstado == ProcessState.RUNNING) {
                    
                    this.setProgramCounter(this.getProgramCounter() + 1);
                    this.setMemoryAddressRegister(this.getMemoryAddressRegister() + 1); // Simulación
                    this.quantumRestante--;
                    
                    System.out.println("  " + name + " (RUN): PC=" + getProgramCounter() + ", Q=" + this.quantumRestante);
                    
                    // Simula 1 ciclo de trabajo
                    Thread.sleep(500); 
                    
                    // --- REVISAR SI DEBE SALIR ---
                    if (this.getProgramCounter() >= this.getTotalInstructions()) {
                        proximoEstado = ProcessState.TERMINATED; // Marcar para terminar
                    
                    } else if (this.isIsIOBound() && (this.getProgramCounter() % this.getIoExceptionCycle() == 0)) {
                        this.setTiempoBloqueadoRestante(this.getIoCompletionCycle());
                        proximoEstado = ProcessState.BLOCKED; // Marcar para bloquearse
                    }
                }
                
                // Si salió por Quantum, marcar para ir a "Listos"
                if (proximoEstado == ProcessState.RUNNING) {
                    proximoEstado = ProcessState.READY;
                }
                
            } finally {
                // 4. ¡CRÍTICO! Liberar la CPU SIN IMPORTAR QUÉ PASÓ.
                System.out.println("Proceso " + name + " LIBERA LA CPU.");
                Interfaz.semaforoCPU.release();
            }
            // --- FIN SECCIÓN CRÍTICA ---

            
            // --- 5. TRANSICIÓN DE ESTADO (Fuera de la CPU) ---
            // El proceso decide a dónde ir AHORA QUE SOLTÓ LA CPU.
            
            if (proximoEstado == ProcessState.TERMINATED) {
                this.setState(ProcessState.TERMINATED);
                System.out.println("Proceso " + name + " -> TERMINADO.");
                Interfaz.semaforoTerminados.acquire();
                try {
                    Interfaz.colaTerminados.insert(this);
                } finally {
                    Interfaz.semaforoTerminados.release();
                }

            } else if (proximoEstado == ProcessState.BLOCKED) {
                this.setState(ProcessState.BLOCKED);
                System.out.println("Proceso " + name + " -> BLOQUEADO.");
                Interfaz.semaforoBloqueados.acquire();
                try {
                    Interfaz.colaBloqueados.insert(this);
                } finally {
                    Interfaz.semaforoBloqueados.release();
                }
                
                // Se duerme, esperando al GestorIO
                synchronized (this) {
                    this.wait(); 
                }
                // Cuando GestorIO lo despierte, el bucle while principal
                // se reiniciará en el estado READY.

            } else if (proximoEstado == ProcessState.READY) {
                this.setState(ProcessState.READY);
                System.out.println("Proceso " + name + " -> LISTOS (por Quantum).");
                
                // Se añade a la cola de Listos
                Interfaz.semaforoListos.acquire();
                try {
                    Interfaz.colaListos.insert(this);
                } finally {
                    Interfaz.semaforoListos.release();
                }
                
                // Se duerme, esperando al Planificador
                synchronized (this) {
                    this.wait(); 
                }
            }
        } // Fin del bucle de vida
        
    } catch (InterruptedException e) {
        this.state = ProcessState.TERMINATED;
        System.out.println("Proceso " + name + " interrumpido y finalizado.");
    }
    System.out.println("--- Hilo de " + name + " ha muerto. ---");
}

    
    // --- GETTERS Y SETTERS (Originales) ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
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

    // --- GETTERS Y SETTERS (Nuevos) ---
    
    public int getTiempoBloqueadoRestante() { return tiempoBloqueadoRestante; }
    public void setTiempoBloqueadoRestante(int t) { this.tiempoBloqueadoRestante = t; }
    public int getQuantumRestante() { return quantumRestante; }
    public void setQuantumRestante(int q) { this.quantumRestante = q; }
    public int getCiclosEnEspera() { return ciclosEnEspera; }
    public void setCiclosEnEspera(int c) { this.ciclosEnEspera = c; }
    
    /** Devuelve las instrucciones que FALTAN por ejecutar (para SRT). */
    public int getInstruccionesRestantes() {
        return this.totalInstructions - this.programCounter;
    }
    
    
}