package os.cpu;

import os.Main;
import os.process.PCB;

import java.util.*;

import static os.constant.CPUConstant.REGISTER_COUNT;

public class CPU {
    private final static CPU INSTANCE = new CPU();
    // 寄存器组
    private final int[] registers = new int[REGISTER_COUNT];
    // 模拟内存（地址 -> 值）
    private final Map<Integer, Integer> memory = new HashMap<>();
    // 指令列表
    private final List<String[]> instructions = new ArrayList<>();
    // 程序计数器
    private int pc;
    //当前进程
    private PCB currentProcess;

    public static CPU getCPU() {
        return INSTANCE;
    }

    // 加载程序到内存
    public void loadProgram(PCB process) {
        //保存上下文
        if (currentProcess != null) {
            currentProcess.setPc(pc);
            currentProcess.updateRegisters(registers);
        }
        currentProcess = process;
        instructions.addAll(process.getInstructions());
        pc = process.getPc();
        changeRegisters(process);
    }

    // 初始化寄存器
    private void changeRegisters(PCB process) {
        for (int i = 0; i < REGISTER_COUNT; i++) {
            registers[i] = process.getRegisters()[i];
        }
    }

    // 执行程序
    public void execute() {
        String[] parts = instructions.get(pc);
        if (Main.debug) {
            System.out.println("Running instruction: " + Arrays.toString(parts));
        }
        switch (parts[0].toLowerCase()) {
            case "mov":
                handleMov(parts);
                break;
            case "syscall":
                handleSyscall(Integer.parseInt(parts[1]));
                break;
            case "nop":
                break;
            case "exit":
        }
        if (Main.debug) {
            printRegisters();
        }
        pc++;
    }

    // 处理MOV指令
    private void handleMov(String[] parts) {
        String dest = parts[1];
        String src = parts[2];

        int value = parseOperand(src);

        if (dest.startsWith("*")) {
            int address = Integer.parseInt(dest.substring(1));
            memory.put(address, value);
        } else if (dest.startsWith("R")) {
            registers[Integer.parseInt(dest.substring(1))] = value;
        }
    }

    // 解析操作数
    private int parseOperand(String operand) {
        if (operand.startsWith("*")) {
            int address = Integer.parseInt(operand.substring(1));
            return memory.getOrDefault(address, 0);
        } else if (operand.startsWith("R")) {
            return registers[Integer.parseInt(operand.substring(1))];
        }
        return Integer.parseInt(operand);
    }

    // 处理系统调用
    private void handleSyscall(int callNumber) {
        switch (callNumber) {
            case 10:
                Kernel.handleMemoryAccess(
                        memory.getOrDefault(1024, 0),
                        memory.getOrDefault(2025, 0),
                        memory.getOrDefault(3026, 0),
                        memory.getOrDefault(4027, 0)
                );
                break;
            default:
                System.out.println("Unsupported syscall: " + callNumber);
        }
    }

    // 获取寄存器状态（调试用）
    public void printRegisters() {
        System.out.println("Register Status:");
        System.out.println(Arrays.toString(registers));
        System.out.println("\n");
    }
}
