package os.test.cpu;

import os.process.*;

import java.util.ArrayList;
import java.util.List;

public class ScriptTest {
    public static void main(String[] args) {
        test(new RoundRobinScheduler(1), "script.txt", "script1.txt");
        test(new RoundRobinScheduler(2), "script.txt", "script1.txt");
        test(new PriorityScheduler(), "script.txt", "script1.txt");
        test(new MLFQScheduler(List.of(
                new MLFQScheduler.QueueConfig(0, 1, true),
                new MLFQScheduler.QueueConfig(1, 2, true),
                new MLFQScheduler.QueueConfig(2, Integer.MAX_VALUE, false)
        )), "script.txt", "script1.txt");

    }

    public static void test(Scheduler scheduler, String... files) {
        System.out.printf("----------------------%sTest begin!----------------------%n", scheduler.getClass().getName());
        List<PCB> pcb = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            pcb.add(ProcessController.createProcess(i, i, files[i]));
        }
        new Simulator(scheduler).simulate(pcb);
    }


}
