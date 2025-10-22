/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class Stats {
    private int completed = 0;
    private long totalTurn = 0;
    private long totalWait = 0;
    private long totalResp = 0;

    public Stats() {}

    public synchronized void recordArrival(PCB p) {}

    public synchronized void recordStart(PCB p, long t) {
        long r = p.getResponse();
        if (r >= 0) totalResp += r;
    }

    public synchronized void recordFinish(PCB p, long t) {
        completed++;
        long ta = p.getTurnaround();
        long wt = p.getWaiting();
        if (ta >= 0) totalTurn += ta;
        if (wt >= 0) totalWait += wt;
    }

    public synchronized int getCompleted() { return completed; }
    public synchronized double avgTurn() { return completed == 0 ? 0 : (double) totalTurn / completed; }
    public synchronized double avgWait() { return completed == 0 ? 0 : (double) totalWait / completed; }
    public synchronized double avgResp() { return completed == 0 ? 0 : (double) totalResp / completed; }

    public String toString() {
        return "done=" + getCompleted() + " avgTurn=" + avgTurn() + " avgWait=" + avgWait() + " avgResp=" + avgResp();
    }
}
