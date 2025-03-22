package os.process;

public class PCB {
    private final int pid;
    private ProcessState state;
    private int priority;
    private final int requiredBurstTime;  // 需要执行的CPU总时间
    private int executedTime;       // 已执行时间

    public PCB(int pid, int priority, int burstTime) {
        this.pid = pid;
        this.state = ProcessState.NEW;
        this.priority = priority;
        this.requiredBurstTime = burstTime;
        this.executedTime = 0;
    }

    // 执行一个时间单元
    public void execute(int timeUnits) {
        executedTime += timeUnits;
    }

    // 检查是否完成
    public boolean isCompleted() {
        return executedTime >= requiredBurstTime;
    }

    // Getters/Setters
    public int getPid() { return pid; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState s) { state = s; }
    public int getRemainingTime() {
        return requiredBurstTime - executedTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getRequiredBurstTime() {
        return requiredBurstTime;
    }

    public int getExecutedTime() {
        return executedTime;
    }

    public void setExecutedTime(int executedTime) {
        this.executedTime = executedTime;
    }
}