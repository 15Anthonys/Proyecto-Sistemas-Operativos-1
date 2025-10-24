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

    public FeedbackAlgorithm(){}
    public FeedbackAlgorithm(int q){ this.qBase = Math.max(1,q); }

    public String name(){ return "Feedback"; }
    public boolean isPreemptive(){ return true; }
    public void setQuantum(int q){ this.qBase = Math.max(1,q); }
    public int getQuantum(){ return qBase; }

    public void enqueue(Cola<Process> ignoredReady, Process p, long now){
        if (p.queueLevel < 0 || p.queueLevel > 2) p.queueLevel = 0;
        p.onEnterReady(now);
        if (p.queueLevel == 0) q0.insert(p);
        else if (p.queueLevel == 1) q1.insert(p);
        else q2.insert(p);
        rebuildDisplayReady();
    }

    public Process pickNext(Cola<Process> ignoredReady, long now){
        Process p = q0.isEmpty()? (q1.isEmpty()? q2.pop() : q1.pop()) : q0.pop();
        if (p != null){
            p.onLeaveReady(now);
            int q = (p.queueLevel==0? qBase : (p.queueLevel==1? qBase*2 : qBase*4));
            p.quantumRemaining = q;
        }
        rebuildDisplayReady();
        return p;
    }

    public boolean shouldPreempt(Process running, Cola<Process> ignoredReady, long now){
        return running != null && running.quantumRemaining <= 0;
    }

    public void onQuantumExpired(Process p, long now){
        // demote
        if (p.queueLevel < 2) p.queueLevel++;
        enqueue(null, p, now);
    }

    private void rebuildDisplayReady(){
        // Refleja concatenación q0|q1|q2 en la cola global Interfaz.colaListos para la UI
        try { Interfaz.semaforoListos.acquire(); } catch (InterruptedException e){ Thread.currentThread().interrupt(); return; }
        try {
            // limpia la cola global reconstruyéndola
            // (pop hasta vaciar)
            while(!Interfaz.colaListos.isEmpty()) Interfaz.colaListos.pop();
            dump(q0, Interfaz.colaListos);
            dump(q1, Interfaz.colaListos);
            dump(q2, Interfaz.colaListos);
        } finally { Interfaz.semaforoListos.release(); }
    }
    private void dump(Cola<Process> from, Cola<Process> to){
        int n = from.getSize();
        while(n-- > 0){
            Process p = from.pop();
            to.insert(p);
            from.insert(p);
        }
    }
}
