/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package PlanificadorYAlgoritmosPlanificadores;

import ProccesFabrication.PCB;
import ProccesFabrication.ProcessState;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Nodo;

/**
 *
 * @author dugla
 */
public abstract class Scheduler {
    protected Cola<PCB> readyQueue;
    protected PCB currentProcess;
    protected String schedulerName;
    protected int contextSwitchTime;
    protected boolean isOperatingSystemRunning;
    protected ListaSimple<PCB> completedProcesses;
    protected Cola<PCB> blockedQueue;
    protected Cola<PCB> suspendedReadyQueue;
    protected Cola<PCB> suspendedBlockedQueue;
    
    // Estadísticas
    protected int totalContextSwitches;
    protected long totalSimulationTime;
    protected EventLogger eventLogger;
    
    public Scheduler(String name) {
        this.schedulerName = name;
        this.readyQueue = new Cola<>();
        this.completedProcesses = new ListaSimple<>();
        this.blockedQueue = new Cola<>();
        this.suspendedReadyQueue = new Cola<>();
        this.suspendedBlockedQueue = new Cola<>();
        this.contextSwitchTime = 1;
        this.totalContextSwitches = 0;
        this.totalSimulationTime = 0;
        this.isOperatingSystemRunning = false;
        this.eventLogger = EventLogger.getInstance();
    }
    
    // Métodos abstractos que deben implementar las subclases
    public abstract PCB selectNextProcess();
    public abstract void addProcess(PCB process);
    public abstract void reorganizeQueues();
    public abstract boolean shouldPreemptCurrentProcess();
    
    // Métodos concretos comunes a todos los planificadores
    public void executeCycle() {
        totalSimulationTime++;
        isOperatingSystemRunning = true;
        
        if (currentProcess != null) {
            executeCurrentProcess();
        } else {
            scheduleNextProcess();
        }
        
        updateBlockedProcesses();
        updateSuspendedProcesses();
        
        isOperatingSystemRunning = false;
    }
    
    private void executeCurrentProcess() {
        // Ejecutar instrucción del proceso actual
        currentProcess.executeInstruction();
        eventLogger.log("Proceso " + currentProcess.getProcessName() + 
                       " ejecuta instrucción " + currentProcess.getProgramCounter());
        
        // Verificar si el proceso ha terminado
        if (currentProcess.isFinished()) {
            terminateCurrentProcess();
            return;
        }
        
        // Verificar si genera I/O
        if (currentProcess.shouldGenerateIO()) {
            blockCurrentProcessForIO();
            return;
        }
        
        // Verificar condiciones específicas del planificador
        if (shouldPreemptCurrentProcess()) {
            preemptCurrentProcess();
        }
    }
    
    private void terminateCurrentProcess() {
        currentProcess.setState(ProcessState.TERMINATED);
        currentProcess.setCompletionTime(totalSimulationTime);
        completedProcesses.addAtTheEnd(currentProcess);
        eventLogger.log("Proceso " + currentProcess.getProcessName() + " terminado");
        currentProcess = null;
        totalContextSwitches++;
        scheduleNextProcess();
    }
    
    private void blockCurrentProcessForIO() {
        currentProcess.startIO();
        blockedQueue.insert(currentProcess);
        eventLogger.log("Proceso " + currentProcess.getProcessName() + " bloqueado por I/O");
        currentProcess = null;
        totalContextSwitches++;
        scheduleNextProcess();
    }
    
    private void preemptCurrentProcess() {
        currentProcess.setState(ProcessState.READY);
        readyQueue.insert(currentProcess);
        eventLogger.log("Proceso " + currentProcess.getProcessName() + " preemptado");
        currentProcess = null;
        totalContextSwitches++;
        scheduleNextProcess();
    }
    
    private void scheduleNextProcess() {
        PCB nextProcess = selectNextProcess();
        if (nextProcess != null) {
            currentProcess = nextProcess;
            currentProcess.setState(ProcessState.RUNNING);
            eventLogger.log("Planificador selecciona Proceso " + currentProcess.getProcessName());
        }
    }
    
    private void updateBlockedProcesses() {
        // Crear una cola temporal para procesos que aún están bloqueados
        Cola<PCB> tempBlockedQueue = new Cola<>();
        
        while (!blockedQueue.isEmpty()) {
            PCB process = blockedQueue.pop();
            process.addIOTime(1);
            
            if (process.getTotalIOTime() >= process.getIOCompletionTime()) {
                process.completeIO();
                process.setState(ProcessState.READY);
                readyQueue.insert(process);
                eventLogger.log("Proceso " + process.getProcessName() + " completó I/O");
            } else {
                tempBlockedQueue.insert(process);
            }
        }
        
        // Restaurar los procesos que siguen bloqueados
        blockedQueue = tempBlockedQueue;
    }
    
    private void updateSuspendedProcesses() {
        // Lógica para manejar procesos suspendidos (simplificada)
        // En una implementación real, aquí se manejaría la memoria
    }
    
    // Métodos de acceso para la interfaz gráfica
    public PCB getCurrentProcess() { return currentProcess; }
    public Cola<PCB> getReadyQueue() { return readyQueue; }
    public Cola<PCB> getBlockedQueue() { return blockedQueue; }
    public Cola<PCB> getSuspendedReadyQueue() { return suspendedReadyQueue; }
    public Cola<PCB> getSuspendedBlockedQueue() { return suspendedBlockedQueue; }
    public ListaSimple<PCB> getCompletedProcesses() { return completedProcesses; }
    public boolean isOperatingSystemRunning() { return isOperatingSystemRunning; }
    public long getTotalSimulationTime() { return totalSimulationTime; }
    public String getSchedulerName() { return schedulerName; }
    
    // Métodos para gestión de memoria y suspensión
    public void suspendProcess(PCB process) {
        if (process.getState() == ProcessState.READY) {
            readyQueue.remove(process);
            process.setState(ProcessState.SUSPENDED_READY);
            suspendedReadyQueue.insert(process);
            eventLogger.log("Proceso " + process.getProcessName() + " suspendido (Listo)");
        } else if (process.getState() == ProcessState.BLOCKED) {
            blockedQueue.remove(process);
            process.setState(ProcessState.SUSPENDED_BLOCKED);
            suspendedBlockedQueue.insert(process);
            eventLogger.log("Proceso " + process.getProcessName() + " suspendido (Bloqueado)");
        }
    }
    
    public void resumeProcess(PCB process) {
        if (process.getState() == ProcessState.SUSPENDED_READY) {
            suspendedReadyQueue.remove(process);
            process.setState(ProcessState.READY);
            readyQueue.insert(process);
            eventLogger.log("Proceso " + process.getProcessName() + " reanudado (Listo)");
        } else if (process.getState() == ProcessState.SUSPENDED_BLOCKED) {
            suspendedBlockedQueue.remove(process);
            process.setState(ProcessState.BLOCKED);
            blockedQueue.insert(process);
            eventLogger.log("Proceso " + process.getProcessName() + " reanudado (Bloqueado)");
        }
    }
    
    // Métodos para métricas de rendimiento
    public double getThroughput() {
        if (totalSimulationTime == 0) return 0;
        return (double) completedProcesses.getSize() / totalSimulationTime;
    }
    
    public double getCPUUtilization() {
        // Calcular tiempo que la CPU estuvo ocupada
        long busyTime = 0;
        Nodo<PCB> current = completedProcesses.getpFirst();
        while (current != null) {
            busyTime += current.getData().getTotalCPUTime();
            current = current.getPnext();
        }
        
        if (currentProcess != null) {
            busyTime += currentProcess.getTotalCPUTime();
        }
        
        return (double) busyTime / totalSimulationTime;
    }
    
    public double getAverageResponseTime() {
        if (completedProcesses.isEmpty()) return 0;
        
        long totalResponseTime = 0;
        int count = 0;
        Nodo<PCB> current = completedProcesses.getpFirst();
        while (current != null) {
            totalResponseTime += current.getData().getResponseTime();
            count++;
            current = current.getPnext();
        }
        
        return (double) totalResponseTime / count;
    }
    
    public void logEvent(String message) {
        eventLogger.log(message);
    }
}