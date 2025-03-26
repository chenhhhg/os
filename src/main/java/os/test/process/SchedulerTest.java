package os.test.process;

import os.process.*;

import java.util.Arrays;
import java.util.List;

public class SchedulerTest {
    public static void main(String[] args) {
        MFLQTest();
        RRTest();
        PriorityTest();
    }

    public static void MFLQTest(){
        System.out.println("----------------------MFLQTest begin!----------------------");
        // 配置三级队列：
        // Q0（最高优先级）：时间片2 ticks，允许抢占
        // Q1：时间片4 ticks，允许抢占
        // Q2（最低优先级）：FCFS，不抢占
        List<MLFQScheduler.QueueConfig> configs = Arrays.asList(
                new MLFQScheduler.QueueConfig(0, 2, true),
                new MLFQScheduler.QueueConfig(1, 4, true),
                new MLFQScheduler.QueueConfig(2, Integer.MAX_VALUE, false)
        );

        Scheduler scheduler = new MLFQScheduler(configs);
        Simulator simulator = new Simulator(scheduler);

        List<PCB> processes = Arrays.asList(
                new PCB(1, 0, 8),  // 长进程
                new PCB(2, 0, 3),  // 短进程
                new PCB(3, 0, 5)   // 中等进程
        );

        simulator.simulate(processes);
    }

    public static void RRTest(){
        System.out.println("----------------------RRTest begin!----------------------");
        Simulator simulator = new Simulator(new RoundRobinScheduler(2));

        List<PCB> processes = Arrays.asList(
                new PCB(1, 0, 8),  // 长进程
                new PCB(2, 0, 3),  // 短进程
                new PCB(3, 0, 5)   // 中等进程
        );

        simulator.simulate(processes);
    }

    public static void PriorityTest(){
        System.out.println("----------------------PriorityTest begin!----------------------");
        Simulator simulator = new Simulator(new PriorityScheduler());

        List<PCB> processes = Arrays.asList(
                new PCB(1, 2, 8),  // 长进程
                new PCB(2, 0, 3),  // 短进程
                new PCB(3, 4, 5)   // 中等进程
        );

        simulator.simulate(processes);
    }
}
