/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class SRTF {
    private final SRReadyQueue q = new SRReadyQueue();
    private volatile boolean running = false;
    private Thread dispatcher;
    private final Stats stats = new Stats();
    private volatile PCB current = null;
    private final long step;

    public SRTF(long step) { this.step = step; }

    public void start() {
        running = true;
        dispatcher = new Thread(new Runnable() {
            public void run() {
                while (running || q.size() > 0 || current != null) {
                    try {
                        if (current == null) {
                            current = q.dequeue();
                            current.setState(PCB.State.RUNNING);
                            long now = System.currentTimeMillis();
                            current.markStart(now);
                            stats.recordStart(current, now);
                        }
                        long run = Math.min(step, current.getRemaining());
                        try { Thread.sleep(run); } catch (InterruptedException e) {}
                        synchronized (current) {
                            current.setRemaining(Math.max(0, current.getRemaining() - run));
                            if (current.getRemaining() == 0) {
                                long fin = System.currentTimeMillis();
                                current.markFinish(fin);
                                current.setState(PCB.State.TERMINATED);
                                stats.recordFinish(current, fin);
                                System.out.println("[SRTF] termina " + current.getPid());
                                current = null;
                            } else {
                                PCB head = q.peek();
                                if (head != null && head.getRemaining() < current.getRemaining()) {
                                    current.setState(PCB.State.READY);
                                    q.enqueueOrdered(current);
                                    System.out.println("[SRTF] preempt " + current.getPid());
                                    current = null;
                                }
                            }
                        }
                    } catch (InterruptedException e) { break; }
                }
            }
        }, "srtf-dispatcher");
        dispatcher.start();
    }

    public void stop() {
        running = false;
        if (dispatcher != null) dispatcher.interrupt();
    }

    public void addProcess(PCB p) {
        p.setState(PCB.State.READY);
        q.enqueueOrdered(p);
        stats.recordArrival(p);
        System.out.println("[SRTF] enqueue " + p.getPid());
        if (current != null && p.getRemaining() < current.getRemaining()) {
            if (dispatcher != null) dispatcher.interrupt();
        }
    }

    public Stats getStats() { return stats; }
}
