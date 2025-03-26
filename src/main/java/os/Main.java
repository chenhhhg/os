package os;

import os.process.*;

import java.util.List;

public class Main {

    public static boolean debug = true;

    public static void main(String[] args) {
        PCB process = ProcessController.createProcess(0, 0, "script.txt");
        Scheduler scheduler = new RoundRobinScheduler(2);

        Simulator simulator = new Simulator(scheduler);

        simulator.simulate(List.of(process));

    }

}
