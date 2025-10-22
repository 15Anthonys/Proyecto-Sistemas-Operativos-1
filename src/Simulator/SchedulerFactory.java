/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Simulator;

/**
 *
 * @author luisg
 */
public class SchedulerFactory {
    public static FCFS createFCFS() {
        return new FCFS();
    }

    public static RoundRobin createRoundRobin(long quantum) {
        return new RoundRobin(quantum);
    }

    public static SRTF createSRTF(long step) {
        return new SRTF(step);
    }
}
