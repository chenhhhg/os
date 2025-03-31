package os.process;

import os.cpu.CPU;

import java.util.ArrayList;
import java.util.List;

import static os.constant.CPUConstant.REGISTER_COUNT;

public class PCB {
    private final int pid;
    private ProcessState state;
    private int priority;
    private final int requiredBurstTime;  // 需要执行的CPU总时间
    private int executedTime;       // 已执行时间

    // 指令列表 todo 改成内存地址
    private final List<String[]> instructions;
    // 程序计数器
    private int pc;
    //寄存器
    private int[] registers = new int[REGISTER_COUNT];

    //for test
    public PCB(int pid, int priority, int burstTime) {
        List<String[]> list = new ArrayList<>();
        for (int i = 0; i < burstTime; i++) {
            list.add(new String[]{"nop"});
        }
        this.pid = pid;
        this.state = ProcessState.NEW;
        this.priority = priority;
        this.requiredBurstTime = burstTime;
        this.executedTime = 0;
        this.instructions = list;
        this.pc = 0;
    }

    public PCB(int pid, int priority, int burstTime, List<String[]> instructions) {
        this.pid = pid;
        this.state = ProcessState.NEW;
        this.priority = priority;
        this.requiredBurstTime = burstTime;
        this.executedTime = 0;
        this.instructions = instructions;
        this.pc = 0;
    }

    // 执行一个时间单元
    public void execute(int clock) {
        CPU cpu = CPU.getCPU();
        cpu.loadProgram(this);
        for (int i = pc; i < instructions.size() && clock > 0;
             ++i, --clock, ++executedTime) {
            cpu.execute();
        }
    }

    // 检查是否完成
    public boolean isCompleted() {
        return instructions.size() == CPU.getCPU().getPc();
    }

    public void updateRegisters(int[] registers) {
        System.arraycopy(registers, 0,
                this.registers, 0,
                REGISTER_COUNT);
    }

    // Getters/Setters
    public int getPid() { return pid; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState s) { state = s; }
    public int getRemainingTime() {
        return instructions.size() - CPU.getCPU().getPc();
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

    public int getPc() {
        return pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public List<String[]> getInstructions() {
        return instructions;
    }

    public int[] getRegisters() {
        return registers;
    }

    public void setRegisters(int[] registers) {
        this.registers = registers;
    }
}