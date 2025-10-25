package Planificacion;

import ProccesFabrication.Process;
import EstructurasDeDatos.Cola;
import EstructurasDeDatos.Nodo;

public class SPNAlgorithm implements SchedulerAlgorithm {
    public String name(){ return "SPN"; }
    public boolean isPreemptive(){ return false; }
    public void setQuantum(int q){}
    public int getQuantum(){ return 0; }

    public void enqueue(Cola<Process> ready, Process p, long now){
        // Insert al final; selección hará rotación para el más corto
        p.onEnterReady(now);
        ready.insert(p);
    }

    public Process pickNext(Cola<Process> ready, long now){
        if (ready.isEmpty()) return null;

        // Encuentra el proceso con menor totalInstructions
        Process best = null;
        int bestLen = Integer.MAX_VALUE;
        Nodo<Process> cur = ready.getpFirst();
        while(cur!=null){
            Process p = cur.getData();
            if (p.getTotalInstructions() < bestLen){
                bestLen = p.getTotalInstructions();
                best = p;
            }
            cur = cur.getPnext();
        }
        // Rotar hasta traerlo a la cabeza y pop
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