package os;

import os.process.*;

import java.util.List;
import static os.process.ProcessController.createProcess;
public class Main {

    public static boolean debug = false;

    public static void main(String[] args) {
        Scheduler scheduler = new RoundRobinScheduler(2);

        Simulator simulator = new Simulator(scheduler);

        simulator.simulate(
            List.of(
                createProcess(0, 0, "write_file.txt"),
                createProcess(1, 1, "script.txt")
        ));

    }

}
