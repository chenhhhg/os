package os.process;

import os.Main;
import os.system.SystemTimer;

import java.util.*;

public class MLFQScheduler implements Scheduler {
    // 队列配置：优先级、时间片、是否抢占
    public record QueueConfig(int priority, int quantum, boolean preemptive) {}

    private final List<Queue<MLFQProcess>> queues = new ArrayList<>();
    private final List<QueueConfig> configs;
    private MLFQProcess currentProcess;
    private int currentQueueLevel = -1;

    // 封装进程的调度信息
    private class MLFQProcess {
        final PCB pcb;
        int queueLevel;
        int remainingQuantum;

        MLFQProcess(PCB pcb) {
            this.pcb = pcb;
            this.queueLevel = 0; // 初始进入最高优先级队列
            this.remainingQuantum = configs.getFirst().quantum();
        }
    }

    public MLFQScheduler(List<QueueConfig> configs) {
        this.configs = new ArrayList<>(configs);
        // 初始化队列（按优先级从高到低排序，数字小代表高优先级）
        configs.stream()
                .sorted(Comparator.comparingInt(QueueConfig::priority))
                .forEach(c -> queues.add(new LinkedList<>()));
    }

    @Override
    public boolean onTick(SystemTimer timer) {
        if (Main.debug){
            for (int i = 0; i < queues.size(); i++) {
                System.out.println("queue: "+i);
                Queue<MLFQProcess> queue = queues.get(i);
                for (MLFQProcess process : queue) {
                    System.out.println(process.pcb.getPid());
                }
            }
        }

        if (currentProcess == null) return true;

        // 执行时间单元
        currentProcess.pcb.execute(1);
        currentProcess.remainingQuantum--;

        // 检查进程完成
        if (currentProcess.pcb.isCompleted()) {
            currentProcess.pcb.setState(ProcessState.TERMINATED);
            currentProcess = null;
            return true;
        }

        // 检查时间片耗尽
        if (currentProcess.remainingQuantum <= 0) {
            demoteProcess(currentProcess);
            currentProcess = null;
            return true;
        }

        // 检查是否需要抢占
        if (shouldPreempt()) {
            requeueCurrentProcess();
            currentProcess = null;
            return true;
        }
        return false;
    }

    private void demoteProcess(MLFQProcess proc) {
        if (proc.queueLevel < queues.size() - 1) {
            proc.queueLevel++;
            proc.remainingQuantum = getQueueQuantum(proc.queueLevel);
        }
        addToQueue(proc);
    }

    private void addToQueue(MLFQProcess proc) {
        proc.pcb.setState(ProcessState.READY);
        queues.get(proc.queueLevel).add(proc);
    }

    private boolean shouldPreempt() {
        // 检查是否有更高优先级队列中存在进程
        for (int i = 0; i < currentQueueLevel; i++) {
            if (!queues.get(i).isEmpty()) {
                return configs.get(i).preemptive();
            }
        }
        return false;
    }

    private void requeueCurrentProcess() {
        addToQueue(currentProcess);
        currentProcess = null;
    }

    @Override
    public void addProcess(PCB pcb) {
        MLFQProcess proc = new MLFQProcess(pcb);
        addToQueue(proc);
    }

    @Override
    public PCB getRunningProcess() {
        return currentProcess != null ? currentProcess.pcb : null;
    }

    @Override
    public void performContextSwitch() {
        if (currentProcess != null) return;

        // 从最高优先级非空队列选择进程
        for (int i = 0; i < queues.size(); i++) {
            Queue<MLFQProcess> q = queues.get(i);
            if (!q.isEmpty()) {
                currentProcess = q.poll();
                currentQueueLevel = i;
                currentProcess.pcb.setState(ProcessState.RUNNING);
                return;
            }
        }
    }

    private int getQueueQuantum(int level) {
        return configs.get(level).quantum();
    }

    // 用于监控显示
    public String getQueueStatus() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < queues.size(); i++) {
            sb.append(String.format("Q%d(%d): %d ",
                    i, configs.get(i).quantum(), queues.get(i).size()));
        }
        return sb.toString();
    }
}
