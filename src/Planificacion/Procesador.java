package Planificacion;

// Importa tus clases
import ProccesFabrication.Process;
import ProccesFabrication.ProcessState;

/**
 * Simula la CPU. Sostiene el proceso en ejecución y ejecuta un ciclo.
 */
public class Procesador {
    
    private Process procesoEnEjecucion;
    
    public Procesador() {
        this.procesoEnEjecucion = null;
    }
    
    public boolean estaLibre() {
        return this.procesoEnEjecucion == null;
    }
    
    public Process getProcesoEnEjecucion() {
        return this.procesoEnEjecucion;
    }
    
    /**
     * "Carga" un proceso en la CPU y actualiza su estado.
     */
    public void setProcesoEnEjecucion(Process p) {
        this.procesoEnEjecucion = p;
        if (p != null) {
            // ¡Importante! Actualiza el estado del proceso
            p.setState(ProcessState.RUNNING); 
        }
    }
    
    /**
     * "Quita" el proceso de la CPU.
     * @return El proceso que estaba en ejecución.
     */
    public Process quitarProcesoActual() {
        Process p = this.procesoEnEjecucion;
        this.procesoEnEjecucion = null;
        return p;
    }
    
    /**
     * Simula un "ciclo" de trabajo de la CPU.
     * Aquí es donde el PC y MAR se actualizan.
     */
    public void ejecutarCiclo() {
        if (estaLibre()) {
            return; // No hay nada que ejecutar
        }
        
        // 1. Simula la ejecución de una instrucción (avanza el PC)
        int pcActual = procesoEnEjecucion.getProgramCounter();
        procesoEnEjecucion.setProgramCounter(pcActual + 1);
        
        // 2. Simula la actualización del MAR (apunta a la siguiente dirección)
        // (Esto es una simulación simple, puedes basarlo en una "dirección base" si quieres)
        int marActual = procesoEnEjecucion.getMemoryAddressRegister();
        procesoEnEjecucion.setMemoryAddressRegister(marActual + 1); // Simulación simple
    }
}