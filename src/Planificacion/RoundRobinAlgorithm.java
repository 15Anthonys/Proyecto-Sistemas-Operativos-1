package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;

public class RoundRobinAlgorithm implements SchedulerAlgorithm {
    private int quantum;
    public RoundRobinAlgorithm(int quantum){ this.quantum = Math.max(1, quantum); }

    public String name(){ return "Round Robin"; }
    public boolean isPreemptive(){ return true; }
    public void setQuantum(int q){ this.quantum = Math.max(1,q); }
    public int getQuantum(){ return quantum; }

    public void enqueue(Cola<Process> ready, Process p, long now){
        p.onEnterReady(now);
        ready.insert(p);
    }

    public Process pickNext(Cola<Process> ready, long now){
        Process p = ready.pop();
        if (p!=null){
            p.onLeaveReady(now);
            p.quantumRemaining = quantum;
        }
        return p;
    }

    public boolean shouldPreempt(Process running, Cola<Process> ready, long now){
        return running != null && running.quantumRemaining <= 0;
    }
}