package os.test.cpu;

import os.process.*;

import java.util.List;

public class SysCallFileTest {
    public static void main(String[] args) {
        PCB process = ProcessController.createProcess(0, 0, "write_file.txt");
        Scheduler scheduler = new RoundRobinScheduler(2);

        Simulator simulator = new Simulator(scheduler);

        simulator.simulate(List.of(process));
    }
}
