/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class SRReadyQueue {
    private static class Node { PCB p; Node next; Node(PCB p){ this.p = p; } }
    private Node head;
    private int size = 0;

    public SRReadyQueue() {}

    public synchronized void enqueueOrdered(PCB p) {
        Node n = new Node(p);
        if (head == null) {
            head = n;
        } else {
            Node prev = null;
            Node cur = head;
            while (cur != null && cur.p.getRemaining() <= p.getRemaining()) {
                prev = cur;
                cur = cur.next;
            }
            if (prev == null) { n.next = head; head = n; }
            else { prev.next = n; n.next = cur; }
        }
        size++;
        notifyAll();
    }

    public synchronized PCB dequeue() throws InterruptedException {
        while (size == 0) wait();
        PCB r = head.p;
        head = head.next;
        size--;
        return r;
    }

    public synchronized PCB peek() { return head == null ? null : head.p; }
    public synchronized int size() { return size; }

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        Node cur = head;
        while (cur != null) {
            sb.append(cur.p.getPid()).append("(").append(cur.p.getRemaining()).append(") ");
            cur = cur.next;
        }
        return sb.toString();
    }
}
