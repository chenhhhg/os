package os.process;

import os.system.SystemTimer;

import java.util.List;

public class Simulator {
    private final SystemTimer timer = new SystemTimer();
    private final Scheduler scheduler;

    public Simulator(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void simulate(List<PCB> processes) {
        // 初始化进程
        processes.forEach(scheduler::addProcess);

        // 主模拟循环
        while (true) {
            timer.tick();

            // 处理时钟中断
            boolean needsScheduling = scheduler.onTick(timer);

            // 执行上下文切换（如果需要）
            if (needsScheduling || scheduler.getRunningProcess() == null) {
                scheduler.performContextSwitch();
            }

            // 打印系统状态
            printSystemStatus();

            // 检查所有进程完成
            if (allProcessesTerminated(processes)) {
                System.out.println("\nAll processes completed at tick: " + timer.getTicks());
                break;
            }
        }
    }

    private void printSystemStatus() {
        PCB running = scheduler.getRunningProcess();
        String queueInfo = "";
        if (scheduler instanceof MLFQScheduler) {
            queueInfo = ((MLFQScheduler)scheduler).getQueueStatus();
        }
        if (scheduler instanceof RoundRobinScheduler){
            queueInfo = String.valueOf(((RoundRobinScheduler)scheduler).getReadyQueueSize());
        }
        if (scheduler instanceof PriorityScheduler) {
            queueInfo = String.valueOf(((PriorityScheduler) scheduler).getQueueSize());
        }
        System.out.printf("[Tick %d] Running: %s StillNeed: %s | Ready Queues: %s%n",
                timer.getTicks(),
                running != null ? "PID-" + running.getPid() : "None",
                running != null ? running.getRemainingTime() : "None",
                queueInfo);
    }

    private boolean allProcessesTerminated(List<PCB> processes) {
        return processes.stream().allMatch(p -> p.getState() == ProcessState.TERMINATED);
    }
}