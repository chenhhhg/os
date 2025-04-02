package os.process;

import os.system.SystemTimer;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import static os.constant.CPUConstant.CLOCK_PER_TICK;

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


        currentProcess.execute(CLOCK_PER_TICK);

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
            currentProcess.setState(ProcessState.READY);
            queue.offer(currentProcess);
        }
        currentProcess = queue.poll();
        if (currentProcess != null) {
            currentProcess.setState(ProcessState.RUNNING);
        }
    }

    public int getQueueSize() {
        return queue.size();
    }
}
