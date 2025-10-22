/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class PCB {
     public enum State { NEW, READY, RUNNING, BLOCKED, TERMINATED }

    private final int pid;
    private final long arrivalTime;
    private final long burstTime;
    private long remaining;
    private State state;
    private long startTime = -1;
    private long finishTime = -1;

    public PCB(int pid, long arrivalTime, long burstTime) {
        this.pid = pid;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remaining = burstTime;
        this.state = State.NEW;
    }

    public int getPid() { return pid; }
    public long getArrivalTime() { return arrivalTime; }
    public long getBurstTime() { return burstTime; }
    public synchronized long getRemaining() { return remaining; }
    public synchronized void setRemaining(long r) { remaining = r; }
    public synchronized State getState() { return state; }
    public synchronized void setState(State s) { state = s; }
    public synchronized void markStart(long t) { if (startTime == -1) startTime = t; }
    public synchronized void markFinish(long t) { finishTime = t; }
    public synchronized long getStartTime() { return startTime; }
    public synchronized long getFinishTime() { return finishTime; }
    public synchronized long getTurnaround() { return finishTime == -1 ? -1 : finishTime - arrivalTime; }
    public synchronized long getWaiting() { return (finishTime == -1 || startTime == -1) ? -1 : getTurnaround() - burstTime; }
    public synchronized long getResponse() { return startTime == -1 ? -1 : startTime - arrivalTime; }

    public String toString() {
        return "P" + pid + "[arr=" + arrivalTime + ",burst=" + burstTime + ",rem=" + remaining + ",st=" + state + "]";
    }
}

