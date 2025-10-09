/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ProccesFabrication;

/**
 *
 * @author dugla
 */
public class PCB {
    // Información básica del proceso
    private int id;
    private String processName;
    private ProcessState state;
    private int priority;
    
    // Registros del proceso (sin usar Map)
    private int programCounter;
    private int memoryAddressRegister;
    private int accumulator;
    private int baseRegister;
    private int limitRegister;
    private int instructionRegister;
    private int stackPointer;
    private int generalRegister1;
    private int generalRegister2;
    private int generalRegister3;
    private int generalRegister4;
    
    // Información de ejecución
    private int totalInstructions;
    private int instructionsExecuted;
    private int timeQuantum;
    private int remainingQuantum;
    
    // Información de I/O
    private boolean isIOBound;
    private int ioExceptionCycle;
    private int ioCompletionTime;
    private boolean waitingForIO;
    
    // Tiempos para métricas
    private long arrivalTime;
    private long startTime;
    private long completionTime;
    private long totalCPUTime;
    private long totalIOTime;
    
    // Constructor
    public PCB(int id, String processName, int totalInstructions, boolean isIOBound, 
               int ioExceptionCycle, int ioCompletionTime, int priority) {
        this.id = id;
        this.processName = processName;
        this.totalInstructions = totalInstructions;
        this.isIOBound = isIOBound;
        this.ioExceptionCycle = ioExceptionCycle;
        this.ioCompletionTime = ioCompletionTime;
        this.priority = priority;
        
        // Inicializar registros
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
        this.accumulator = 0;
        this.baseRegister = 0;
        this.limitRegister = totalInstructions;
        this.instructionRegister = 0;
        this.stackPointer = 0;
        this.generalRegister1 = 0;
        this.generalRegister2 = 0;
        this.generalRegister3 = 0;
        this.generalRegister4 = 0;
        
        // Inicializar estado
        this.state = ProcessState.NEW;
        this.instructionsExecuted = 0;
        this.waitingForIO = false;
        this.arrivalTime = System.currentTimeMillis();
    }
    
    // Getters para registros
    public int getRegister(String registerName) {
        switch (registerName.toUpperCase()) {
            case "PC": return programCounter;
            case "MAR": return memoryAddressRegister;
            case "ACC": return accumulator;
            case "BASE": return baseRegister;
            case "LIMIT": return limitRegister;
            case "IR": return instructionRegister;
            case "SP": return stackPointer;
            case "R1": return generalRegister1;
            case "R2": return generalRegister2;
            case "R3": return generalRegister3;
            case "R4": return generalRegister4;
            default: return 0;
        }
    }
    
    // Setters para registros
    public void setRegister(String registerName, int value) {
        switch (registerName.toUpperCase()) {
            case "PC": programCounter = value; break;
            case "MAR": memoryAddressRegister = value; break;
            case "ACC": accumulator = value; break;
            case "BASE": baseRegister = value; break;
            case "LIMIT": limitRegister = value; break;
            case "IR": instructionRegister = value; break;
            case "SP": stackPointer = value; break;
            case "R1": generalRegister1 = value; break;
            case "R2": generalRegister2 = value; break;
            case "R3": generalRegister3 = value; break;
            case "R4": generalRegister4 = value; break;
        }
    }
    
    // Métodos para ejecución de instrucciones
    public void executeInstruction() {
        if (programCounter < totalInstructions) {
            instructionsExecuted++;
            programCounter++;
            memoryAddressRegister = programCounter;
            totalCPUTime++;
        }
    }
    
    public boolean isFinished() {
        return instructionsExecuted >= totalInstructions;
    }
    
    public boolean shouldGenerateIO() {
        return isIOBound && instructionsExecuted > 0 && 
               instructionsExecuted % ioExceptionCycle == 0;
    }
    
    // Métodos para gestión de I/O
    public void startIO() {
        waitingForIO = true;
        state = ProcessState.BLOCKED;
    }
    
    public void completeIO() {
        waitingForIO = false;
        state = ProcessState.READY;
    }
    
    // Getters y Setters básicos
    public int getId() { return id; }
    public String getProcessName() { return processName; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getProgramCounter() { return programCounter; }
    public int getMemoryAddressRegister() { return memoryAddressRegister; }
    public int getTotalInstructions() { return totalInstructions; }
    public int getInstructionsExecuted() { return instructionsExecuted; }
    public boolean isIOBound() { return isIOBound; }
    public int getIOExceptionCycle() { return ioExceptionCycle; }
    public int getIOCompletionTime() { return ioCompletionTime; }
    public boolean isWaitingForIO() { return waitingForIO; }
    public int getTimeQuantum() { return timeQuantum; }
    public void setTimeQuantum(int timeQuantum) { 
        this.timeQuantum = timeQuantum; 
        this.remainingQuantum = timeQuantum;
    }
    public int getRemainingQuantum() { return remainingQuantum; }
    public void decrementQuantum() { remainingQuantum--; }
    public void resetQuantum() { remainingQuantum = timeQuantum; }
    
    // Métodos para métricas
    public long getArrivalTime() { return arrivalTime; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    public long getTotalCPUTime() { return totalCPUTime; }
    public long getTotalIOTime() { return totalIOTime; }
    public void addIOTime(long ioTime) { totalIOTime += ioTime; }
    
    // Método para calcular tiempo de respuesta
    public long getResponseTime() {
        return startTime - arrivalTime;
    }
    
    // Método para calcular tiempo de turnaround
    public long getTurnaroundTime() {
        return completionTime - arrivalTime;
    }
    
    // Método para calcular tiempo de espera
    public long getWaitingTime() {
        return getTurnaroundTime() - totalCPUTime - totalIOTime;
    }
    
    @Override
    public String toString() {
        return String.format("PCB[ID=%d, Name=%s, State=%s, PC=%d/%d, IO=%b]", 
                           id, processName, state, instructionsExecuted, totalInstructions, isIOBound);
    }
    
    // Método para obtener todos los registros como array para visualización
    public String[] getRegistersAsArray() {
        return new String[] {
            "PC: " + programCounter,
            "MAR: " + memoryAddressRegister,
            "ACC: " + accumulator,
            "BASE: " + baseRegister,
            "LIMIT: " + limitRegister,
            "IR: " + instructionRegister,
            "SP: " + stackPointer,
            "R1: " + generalRegister1,
            "R2: " + generalRegister2,
            "R3: " + generalRegister3,
            "R4: " + generalRegister4
        };
    }
    
    // Método para obtener información resumida del proceso
    public String getProcessInfo() {
        return String.format(
            "Proceso: %s (ID: %d)\n" +
            "Estado: %s\n" +
            "Instrucciones: %d/%d\n" +
            "Tipo: %s\n" +
            "Prioridad: %d\n" +
            "Quantum restante: %d",
            processName, id, state, instructionsExecuted, totalInstructions,
            isIOBound ? "I/O Bound" : "CPU Bound", priority, remainingQuantum
        );
    }
}
