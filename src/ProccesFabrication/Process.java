/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ProccesFabrication;

/**
 *
 * @author dugla
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
    
    
    private static int nextId = 0; // Variable estática para generar IDs únicos

/**
 * Constructor para crear un nuevo proceso con todos sus datos iniciales.
 */
    public Process(String name, int totalInstructions, boolean isIOBound, int ioExceptionCycle, int ioCompletionCycle) {
        this.id = nextId++; // Asigna un ID único y lo incrementa para el siguiente
        this.name = name;
        this.totalInstructions = totalInstructions;
        this.isIOBound = isIOBound;

        // Asignar valores de E/S solo si el proceso es de ese tipo
        if (isIOBound) {
            this.ioExceptionCycle = ioExceptionCycle;
            this.ioCompletionCycle = ioCompletionCycle;
        } else {
            // Si no es I/O Bound, ponemos valores que no interfieran
            this.ioExceptionCycle = -1;
            this.ioCompletionCycle = -1;
        }

        // Inicializamos los valores por defecto para un proceso nuevo
        this.state = ProcessState.NEW; // Necesitarás un enum ProcessState
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        this.creationTime = System.currentTimeMillis(); // Guarda el momento exacto de creación
        this.completionTime = -1; // Aún no ha terminado
    }

    
      // Getters y setters
    // Implementación de run() para el thread
  

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the state
     */
    public ProcessState getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(ProcessState state) {
        this.state = state;
    }

    /**
     * @return the totalInstructions
     */
    public int getTotalInstructions() {
        return totalInstructions;
    }

    /**
     * @param totalInstructions the totalInstructions to set
     */
    public void setTotalInstructions(int totalInstructions) {
        this.totalInstructions = totalInstructions;
    }

    /**
     * @return the programCounter
     */
    public int getProgramCounter() {
        return programCounter;
    }

    /**
     * @param programCounter the programCounter to set
     */
    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    /**
     * @return the memoryAddressRegister
     */
    public int getMemoryAddressRegister() {
        return memoryAddressRegister;
    }

    /**
     * @param memoryAddressRegister the memoryAddressRegister to set
     */
    public void setMemoryAddressRegister(int memoryAddressRegister) {
        this.memoryAddressRegister = memoryAddressRegister;
    }

    /**
     * @return the isIOBound
     */
    public boolean isIsIOBound() {
        return isIOBound;
    }

    /**
     * @param isIOBound the isIOBound to set
     */
    public void setIsIOBound(boolean isIOBound) {
        this.isIOBound = isIOBound;
    }

    /**
     * @return the ioExceptionCycle
     */
    public int getIoExceptionCycle() {
        return ioExceptionCycle;
    }

    /**
     * @param ioExceptionCycle the ioExceptionCycle to set
     */
    public void setIoExceptionCycle(int ioExceptionCycle) {
        this.ioExceptionCycle = ioExceptionCycle;
    }

    /**
     * @return the ioCompletionCycle
     */
    public int getIoCompletionCycle() {
        return ioCompletionCycle;
    }

    /**
     * @param ioCompletionCycle the ioCompletionCycle to set
     */
    public void setIoCompletionCycle(int ioCompletionCycle) {
        this.ioCompletionCycle = ioCompletionCycle;
    }

    /**
     * @return the creationTime
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime the creationTime to set
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * @return the completionTime
     */
    public long getCompletionTime() {
        return completionTime;
    }

    /**
     * @param completionTime the completionTime to set
     */
    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }
}

