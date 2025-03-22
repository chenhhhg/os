package os.process;

import os.system.SystemTimer;

import java.util.LinkedList;
import java.util.Queue;

public class RoundRobinScheduler implements Scheduler {
    private final Queue<PCB> readyQueue = new LinkedList<>();
    private PCB currentProcess = null;
    private int remainingQuantum;
    private final int timeQuantum;

    public RoundRobinScheduler(int quantum) {
        this.timeQuantum = quantum;
        this.remainingQuantum = quantum;
    }

    @Override
    public boolean onTick(SystemTimer timer) {
        if (currentProcess == null) return true;
        // 执行一个时间单元
        currentProcess.execute(1);
        remainingQuantum--;

        // 检查进程完成
        if (currentProcess.isCompleted()) {
            currentProcess.setState(ProcessState.TERMINATED);
            currentProcess = null;
            return true; // 需要调度
        }

        // 检查时间片耗尽
        if (remainingQuantum == 0) {
            currentProcess.setState(ProcessState.READY);
            readyQueue.add(currentProcess);
            currentProcess = null;
            return true; // 需要调度
        }

        //继续执行
        return false;
    }

    @Override
    public void addProcess(PCB process) {
        process.setState(ProcessState.READY);
        readyQueue.add(process);
    }

    @Override
    public PCB getRunningProcess() {
        return currentProcess;
    }

    @Override
    public void performContextSwitch() {
        if (currentProcess == null && !readyQueue.isEmpty()) {
            currentProcess = readyQueue.poll();
            currentProcess.setState(ProcessState.RUNNING);
            remainingQuantum = timeQuantum; // 重置时间片
        }
    }

    public int getReadyQueueSize(){
        return readyQueue.size();
    }
}
