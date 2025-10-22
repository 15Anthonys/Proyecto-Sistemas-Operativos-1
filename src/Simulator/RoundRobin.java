/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class RoundRobin {
    private final ReadyQueue q = new ReadyQueue();
    private volatile boolean running = false;
    private Thread dispatcher;
    private final Stats stats = new Stats();
    private final long quantum;

    public RoundRobin(long quantum) { this.quantum = quantum; }

    public void start() {
        running = true;
        dispatcher = new Thread(new Runnable() {
            public void run() {
                while (running || q.size() > 0) {
                    PCB p;
                    try { p = q.dequeue(); } catch (InterruptedException e) { break; }
                    if (p == null) continue;
                    p.setState(PCB.State.RUNNING);
                    long now = System.currentTimeMillis();
                    p.markStart(now);
                    stats.recordStart(p, now);
                    long run = Math.min(quantum, p.getRemaining());
                    try { Thread.sleep(run); } catch (InterruptedException e) {}
                    synchronized (p) {
                        p.setRemaining(Math.max(0, p.getRemaining() - run));
                        if (p.getRemaining() == 0) {
                            long fin = System.currentTimeMillis();
                            p.markFinish(fin);
                            p.setState(PCB.State.TERMINATED);
                            stats.recordFinish(p, fin);
                            System.out.println("[RR] termina " + p.getPid());
                        } else {
                            p.setState(PCB.State.READY);
                            q.enqueue(p);
                            System.out.println("[RR] requeue " + p.getPid() + " rem=" + p.getRemaining());
                        }
                    }
                }
            }
        }, "rr-dispatcher");
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
        System.out.println("[RR] enqueue " + p.getPid());
    }

    public Stats getStats() { return stats; }
}
