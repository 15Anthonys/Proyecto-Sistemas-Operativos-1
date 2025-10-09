/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ProccesFabrication;

/**
 *
 * @author dugla
 */
public class PCBExample {
    public static void main(String[] args) {
        // Crear un PCB para un proceso I/O bound
        PCB pcb = new PCB(1, "Proceso1", 100, true, 10, 5, 2);
        pcb.setTimeQuantum(4);
        
        // Establecer algunos registros
        pcb.setRegister("PC", 0);
        pcb.setRegister("ACC", 100);
        pcb.setRegister("R1", 42);
        
        // Ejecutar algunas instrucciones
        for (int i = 0; i < 15; i++) {
            pcb.executeInstruction();
            System.out.println("PC: " + pcb.getProgramCounter() + 
                             ", MAR: " + pcb.getMemoryAddressRegister());
            
            if (pcb.shouldGenerateIO()) {
                System.out.println("Generando excepción de I/O");
                pcb.startIO();
            }
        }
        
        // Mostrar información del proceso
        System.out.println(pcb.getProcessInfo());
        
        // Mostrar registros
        String[] registers = pcb.getRegistersAsArray();
        for (String reg : registers) {
            System.out.println(reg);
        }
    }
}