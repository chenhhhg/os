package os.process;

import os.system.SystemTimer;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class PriorityScheduler implements Scheduler{
    private final Queue<PCB> queue =
            new PriorityQueue<>(Comparator.comparingInt(PCB::getPriority));
    private PCB currentProcess;

    @Override
    public boolean onTick(SystemTimer timer) {
        if (currentProcess == null) return true;
        if (queue.peek() != null &&
                currentProcess.getPriority() > queue.peek().getPriority())
            return true;

        currentProcess.execute(1);

        if (currentProcess.isCompleted()){
            currentProcess.setState(ProcessState.TERMINATED);
            currentProcess = null;
            return true;
        }

        return false;
    }

    @Override
    public void addProcess(PCB process) {
        queue.offer(process);
    }

    @Override
    public PCB getRunningProcess() {
        return currentProcess;
    }

    @Override
    public void performContextSwitch() {
        if (currentProcess != null){
            queue.offer(currentProcess);
        }
        currentProcess = queue.poll();
    }
}
