package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;

public class HRRNAlgorithm implements SchedulerAlgorithm {
    public String name(){ return "HRRN"; }
    public boolean isPreemptive(){ return false; }
    public void setQuantum(int q){}
    public int getQuantum(){ return 0; }

    public void enqueue(Cola<Process> ready, Process p, long now){
        p.onEnterReady(now);
        ready.insert(p);
    }

    public Process pickNext(Cola<Process> ready, long now){
        if (ready.isEmpty()) return null;
        double bestRatio = -1.0;
        Process best = null;
        Nodo<Process> cur = ready.getpFirst();
        while(cur!=null){
            Process p = cur.getData();
            long waiting = now - p.lastReadyEnqueueCycle;
            int service = Math.max(1, p.getTotalInstructions());
            double r = (waiting + service) / (double) service;
            if (r > bestRatio){ bestRatio = r; best = p; }
            cur = cur.getPnext();
        }
        if (best != null){
            int rotates = ready.getSize();
            while(rotates-- > 0){
                Process head = ready.pop();
                if (head == best){
                    best.onLeaveReady(now);
                    return head;
                }
                ready.insert(head);
            }
        }
        return null;
    }

    public boolean shouldPreempt(Process running, Cola<Process> ready, long now){ return false; }
}