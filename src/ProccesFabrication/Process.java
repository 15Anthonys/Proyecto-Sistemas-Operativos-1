package ProccesFabrication;

public class Process implements Runnable {
    private static int NEXT_ID = 1;
    private static synchronized int nextId() { return NEXT_ID++; }

    private final int id;
    private String name;
    private int totalInstructions;
    private int remainingInstructions;
    private boolean isIOBound;
    private int exceptionPeriod;   // cada cuántos ciclos genera I/O (si IO-bound)
    private int exceptionService;  // ciclos de servicio de la I/O

    // Registros (modelo simple lineal)
    private int programCounter;
    private int memoryAddressRegister;

    private volatile ProcessState state = ProcessState.NEW;

    // Métricas
    public long arrivalCycle = 0;
    public long firstRunCycle = -1;
    public long finishCycle = -1;
    public long lastReadyEnqueueCycle = 0;
    public long totalWaitingCycles = 0;

    // RR/MLFQ
    public int quantumRemaining = 0;
    public int queueLevel = 0; // MLFQ

    // Ejecución
    public int executedCycles = 0;

    public Process(String name, int instructions, boolean isIOBound, int exceptionPeriod, int exceptionService){
        this.id = nextId();
        this.name = name;
        this.totalInstructions = instructions;
        this.remainingInstructions = instructions;
        this.isIOBound = isIOBound;
        this.exceptionPeriod = exceptionPeriod;
        this.exceptionService = exceptionService;
    }

    public void onEnterReady(long now){
        state = ProcessState.READY;
        lastReadyEnqueueCycle = now;
    }
    public void onLeaveReady(long now){
        if (now >= lastReadyEnqueueCycle) {
            totalWaitingCycles += (now - lastReadyEnqueueCycle);
        }
    }

    public boolean isFinished(){ return remainingInstructions <= 0; }

    public boolean shouldRaiseIO(){
        if (!isIOBound) return false;
        if (exceptionPeriod <= 0) return false;
        return executedCycles > 0 && (executedCycles % exceptionPeriod == 0);
    }

    // Ejecuta 1 instrucción
    public void step(){
        programCounter++;
        memoryAddressRegister++;
        executedCycles++;
        remainingInstructions--;
    }

    @Override
    public void run() {
        // Hilo pasivo (modelo): existe para cumplir el requerimiento
        // de "usar threads por proceso". La CPU (Planificador) avanza el proceso.
        while(state != ProcessState.TERMINATED){
            try { Thread.sleep(50); } catch (InterruptedException ignored){}
        }
    }

    // Getters/setters para la UI
    public int getId(){ return id; }
    public String getName(){ return name; }
    public int getProgramCounter(){ return programCounter; }
    public int getMemoryAddressRegister(){ return memoryAddressRegister; }
    public ProcessState getState(){ return state; }
    public void setState(ProcessState s){ this.state = s; }
    public boolean isIsIOBound(){ return isIOBound; }
    public int getExceptionPeriod(){ return exceptionPeriod; }
    public int getExceptionService(){ return exceptionService; }
    public int getTotalInstructions(){ return totalInstructions; }
    public int getRemainingInstructions(){ return remainingInstructions; }
    public void setRemainingInstructions(int r){ this.remainingInstructions = r; }
}