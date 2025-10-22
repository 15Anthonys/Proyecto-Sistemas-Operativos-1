/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package PlanificadorYAlgoritmosPlanificadores;

import EstructurasDeDatos.ListaSimple;

/**
 *
 * @author dugla
 */
public class EventLogger {
    private static EventLogger instance;
    private ListaSimple<String> logEntries;
    
    private EventLogger() {
        logEntries = new ListaSimple<>();
    }
    
    public static EventLogger getInstance() {
        if (instance == null) {
            instance = new EventLogger();
        }
        return instance;
    }
    
    public void log(String message) {
        String timestamp = "[" + System.currentTimeMillis() + "] ";
        logEntries.addAtTheEnd(timestamp + message);
        System.out.println(timestamp + message); // También imprimir en consola
    }
    
    public ListaSimple<String> getLogEntries() {
        return logEntries;
    }
    
    public void clearLog() {
        logEntries.wipeList();
    }
}
