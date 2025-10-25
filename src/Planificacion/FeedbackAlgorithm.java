/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;
import soplanificacion.Interfaz;

public class FeedbackAlgorithm implements SchedulerAlgorithm {
    private final Cola<Process> q0 = new Cola<>();
    private final Cola<Process> q1 = new Cola<>();
    private final Cola<Process> q2 = new Cola<>();
    private int qBase = 3;

    // --- Constructor, name, isPreemptive, set/getQuantum (sin cambios) ---
    public FeedbackAlgorithm(){}
    public FeedbackAlgorithm(int q){ this.qBase = Math.max(1,q); }
    public String name(){ return "Feedback"; }
    public boolean isPreemptive(){ return true; }
    public void setQuantum(int q){ this.qBase = Math.max(1,q); }
    public int getQuantum(){ return qBase; }

    // --- enqueue (sin cambios respecto a la versión anterior) ---
     public void enqueue(Cola<Process> ignoredReady, Process p, long now){
        if (p.queueLevel < 0 || p.queueLevel > 2) p.queueLevel = 0;
        p.onEnterReady(now);
        if (p.queueLevel == 0) q0.insert(p);
        else if (p.queueLevel == 1) q1.insert(p);
        else q2.insert(p);
        // rebuildDisplayReady(); // Correcto que esté comentado
    }

    /**
     * Selecciona el siguiente proceso NO TERMINADO de la cola de mayor prioridad.
     * Descarta silenciosamente los procesos terminados ("zombis") que encuentre.
     * <<< CORRECCIÓN PARA EVITAR DUPLICADOS EN TERMINADOS >>>
     */
    public Process pickNext(Cola<Process> ignoredReady, long now){
        Process p = null;
        
        // Bucle para buscar un proceso VÁLIDO (no terminado)
        while (true) { 
            // Intenta sacar de la cola de mayor prioridad disponible
            if (!q0.isEmpty()) {
                p = q0.pop();
            } else if (!q1.isEmpty()) {
                p = q1.pop();
            } else if (!q2.isEmpty()) {
                p = q2.pop();
            } else {
                p = null; // Todas las colas internas están vacías
            }

            // Si no encontramos proceso (colas vacías) o el que encontramos NO está terminado...
            if (p == null || !p.isFinished()) {
                break; // ...salimos del bucle (encontramos uno bueno o no hay más)
            } 
            // else { 
                 // Si p SÍ está terminado (es un zombi), lo ignoramos.
                 // El bucle continúa buscando en la siguiente iteración.
                 // No lo volvemos a encolar, efectivamente lo descartamos.
                 // Interfaz.logEvento("Feedback.pickNext: Descartando zombi " + p.getName()); // Log opcional
            // }
        }

        // Si encontramos un proceso válido (p no es null y no está terminado)
        if (p != null){
            p.onLeaveReady(now);
            // Asigna quantum según el nivel del proceso
            int q = (p.queueLevel==0? qBase : (p.queueLevel==1? qBase*2 : qBase*4));
            p.quantumRemaining = q;
        }
        
        // rebuildDisplayReady(); // Sigue comentado aquí, es correcto
        
        return p; // Devuelve el proceso válido encontrado, o null si no había
    }

    // --- shouldPreempt (sin cambios) ---
    public boolean shouldPreempt(Process running, Cola<Process> ignoredReady, long now){
        return running != null && running.quantumRemaining <= 0;
    }

    // --- onQuantumExpired (sin cambios respecto a la versión anterior) ---
    // (Ya tiene la comprobación !p.isFinished() antes de enqueue)
    public void onQuantumExpired(Process p, long now){
        if (!p.isFinished()) {
            if (p.queueLevel < 2) p.queueLevel++;
            enqueue(null, p, now);
            Interfaz.logEvento("Feedback: Proceso " + p.getName() + " degradado a nivel " + p.queueLevel + ".");
        } else {
             Interfaz.logEvento("Feedback: Proceso " + p.getName() + " terminó durante quantum expiration, no re-encolar.");
        }
        rebuildDisplayReady(); // Llama a rebuild aquí (seguro)
    }

    // --- rebuildDisplayReady (sin cambios) ---
    public void rebuildDisplayReady(){
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); return; }
        try {
            while(!Interfaz.colaListos.isEmpty()) Interfaz.colaListos.pop();
            dump(q0, Interfaz.colaListos); // Llama al dump corregido
            dump(q1, Interfaz.colaListos); // Llama al dump corregido
            dump(q2, Interfaz.colaListos); // Llama al dump corregido
        } finally { Interfaz.semaforoListos.release(); }
    }
    
    /**
     * Método auxiliar para copiar el contenido de una cola interna ('from')
     * a la cola global VISUAL ('to') sin vaciar la cola interna y
     * ***SALTÁNDOSE LOS PROCESOS YA TERMINADOS***. 
     * <<< CORRECCIÓN PARA EVITAR FANTASMAS VISUALES >>>
     */
    private void dump(Cola<Process> from, Cola<Process> to){
        int n = from.getSize();
        while(n-- > 0){
            Process p = from.pop(); // Saca el primero
            if (p != null) {
                // Solo inserta en la cola VISUAL ('to') si el proceso NO está terminado
                if (!p.isFinished()) {
                    to.insert(p); 
                }
                // Siempre lo vuelve a insertar al final de 'from' para mantener la cola interna intacta
                from.insert(p); 
            }
        }
    }
}