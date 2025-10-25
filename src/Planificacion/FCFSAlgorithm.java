package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;

public class FCFSAlgorithm implements SchedulerAlgorithm {
    public String name(){ return "FCFS"; }
    public boolean isPreemptive(){ return false; }
    public void setQuantum(int q){}
    public int getQuantum(){ return 0; }

    public void enqueue(Cola<Process> ready, Process p, long now){
        p.onEnterReady(now);
        ready.insert(p);
    }

    public Process pickNext(Cola<Process> ready, long now){
        Process p = ready.pop();
        if (p != null) p.onLeaveReady(now);
        return p;
    }

    public boolean shouldPreempt(Process running, Cola<Process> ready, long now){ return false; }
}