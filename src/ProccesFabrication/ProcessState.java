/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ProccesFabrication;

/**
 *
 * @author dugla
 */


public enum ProcessState {
    NEW("Nuevo"),
    READY("Listo"),
    RUNNING("Ejecutando"),
    BLOCKED("Bloqueado"),
    TERMINATED("Terminado"),
    SUSPENDED_READY("Suspendido Listo"),
    SUSPENDED_BLOCKED("Suspendido Bloqueado");
    
    private final String description;
    
    ProcessState(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}
