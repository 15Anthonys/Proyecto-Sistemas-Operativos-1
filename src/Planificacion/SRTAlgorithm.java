package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;

public class SRTAlgorithm implements SchedulerAlgorithm {
    public String name(){ return "SRT"; }
    public boolean isPreemptive(){ return true; }
    public void setQuantum(int q){}
    public int getQuantum(){ return 0; }

    public void enqueue(Cola<Process> ready, Process p, long now){
        p.onEnterReady(now);
        ready.insert(p);
    }

    public Process pickNext(Cola<Process> ready, long now){
        if (ready.isEmpty()) return null;
        Process best = null;
        int bestRem = Integer.MAX_VALUE;
        Nodo<Process> cur = ready.getpFirst();
        while(cur!=null){
            Process p = cur.getData();
            if (p.getRemainingInstructions() < bestRem){
                bestRem = p.getRemainingInstructions(); best = p;
            }
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

    public boolean shouldPreempt(Process running, Cola<Process> ready, long now){
        if (running == null) return false;
        // ¿Hay alguien con menor remaining en ready?
        Nodo<Process> cur = ready.getpFirst();
        while(cur!=null){
            Process p = cur.getData();
            if (p.getRemainingInstructions() < running.getRemainingInstructions()) return true;
            cur = cur.getPnext();
        }
        return false;
    }
}