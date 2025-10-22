/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public abstract class Scheduler {
    protected Stats stats = new Stats();

    public abstract void start();
    public abstract void stop();
    public abstract void addProcess(PCB p);

    public Stats getStats() {
        return stats;
    }
}
