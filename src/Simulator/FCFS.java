/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class FCFS {
    private final ReadyQueue q = new ReadyQueue();
    private volatile boolean running = false;
    private Thread dispatcher;
    private final Stats stats = new Stats();

    public FCFS() {}

    public void start() {
        running = true;
        dispatcher = new Thread(new Runnable() {
            public void run() {
                while (running || q.size() > 0) {
                    PCB p;
                    try { p = q.dequeue(); } catch (InterruptedException e) { break; }
                    p.setState(PCB.State.RUNNING);
                    long now = System.currentTimeMillis();
                    p.markStart(now);
                    stats.recordStart(p, now);
                    long toRun;
                    synchronized (p) { toRun = p.getRemaining(); }
                    try { Thread.sleep(toRun); } catch (InterruptedException e) {}
                    long finish = System.currentTimeMillis();
                    synchronized (p) { p.setRemaining(0); p.markFinish(finish); p.setState(PCB.State.TERMINATED); }
                    stats.recordFinish(p, finish);
                    System.out.println("[FCFS] termina " + p.getPid());
                }
            }
        }, "fcfs-dispatcher");
        dispatcher.start();
    }

    public void stop() {
        running = false;
        if (dispatcher != null) dispatcher.interrupt();
    }

    public void addProcess(PCB p) {
        p.setState(PCB.State.READY);
        q.enqueue(p);
        stats.recordArrival(p);
        System.out.println("[FCFS] enqueue " + p.getPid());
    }

    public Stats getStats() { return stats; }
}
