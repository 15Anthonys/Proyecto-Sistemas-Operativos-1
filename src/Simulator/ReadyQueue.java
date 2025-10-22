/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class ReadyQueue {
    private static class Node { PCB p; Node next; Node(PCB p){ this.p = p; } }
    private Node head;
    private Node tail;
    private int size = 0;

    public ReadyQueue() {}

    public synchronized void enqueue(PCB p) {
        Node n = new Node(p);
        if (tail == null) {
            head = tail = n;
        } else {
            tail.next = n;
            tail = n;
        }
        size++;
        notifyAll();
    }

    public synchronized PCB dequeue() throws InterruptedException {
        while (size == 0) wait();
        PCB r = head.p;
        head = head.next;
        if (head == null) tail = null;
        size--;
        return r;
    }

    public synchronized int size() { return size; }

    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        Node cur = head;
        while (cur != null) {
            sb.append(cur.p.getPid()).append(" ");
            cur = cur.next;
        }
        return sb.toString();
    }
}
