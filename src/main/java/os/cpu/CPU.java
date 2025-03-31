package os.cpu;

import os.Main;
import os.filesystem.FileDescriptor;
import os.filesystem.FileSystem;
import os.filesystem.FileTreeNode;
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
    private List<String[]> instructions = new ArrayList<>();
    // 程序计数器
    private int pc;
    //当前进程
    private PCB currentProcess;
    private final FileSystem fileSystem = FileSystem.getInstance();

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
        instructions = process.getInstructions();
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
            case "add":
                handleAdd(parts);
                break;
            case "bnez":
                handleBnez(parts);
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

    private void handleBnez(String[] parts) {
        String dest = parts[1];
        String src = parts[2];
        int i = parseOperand(dest);
        if (i != 0) {
            pc += parseOperand(src);
        }
    }

    private void handleAdd(String[] parts) {
        String dest = parts[1];
        String src1 = parts[2];
        String src2 = parts[3];
        int value1 = parseOperand(src1);
        int value2 = parseOperand(src2);
        int result = value1 + value2;
        if (dest.startsWith("*")) {
            int address = Integer.parseInt(dest.substring(1));
            memory.put(address, result);
        } else if (dest.startsWith("R")) {
            registers[Integer.parseInt(dest.substring(1))] = result;
        }
    }

    // 处理MOV指令
    private void handleMov(String[] parts) {
        String dest = parts[1];
        String src = parts[2];
        if (src.matches("[a-zA-Z]+")){
            int address = Integer.parseInt(dest.substring(1));
            for (char c : src.toCharArray()) {
                parts[2] = c + "";
                parts[1] = "*" + address;
                handleMovSingle(parts);
                address+=2;
            }
        }else {
            handleMovSingle(parts);
        }
    }

    private void handleMovSingle(String[] parts) {
        String dest = parts[1];
        String src = parts[2];

        int value = parseOperand(src);

        if (dest.startsWith("*")) {
            if (dest.substring(1).startsWith("R")){
                int address = registers[Integer.parseInt(dest.substring(2))];
                memory.put(address, value);
            }else {
                int address = Integer.parseInt(dest.substring(1));
                memory.put(address, value);
            }
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
        }else if (operand.matches("[a-zA-Z]+")) {
            return  operand.charAt(0);
        }
        return Integer.parseInt(operand);
    }

    // 处理系统调用
    private void handleSyscall(int callNumber) {
        String path;
        String name;
        FileDescriptor fd;
        Object[] objects;
        switch (callNumber) {
            //open file
            case 3:
                path = readString(registers[2]);
                name = readString(registers[3]);
                fd = fileSystem.openFile(path + "/" + name, currentProcess.getPid(), true, true);
                registers[1] = fd.getFdId();
                break;
            //close file
            case 4:
                fd = fileSystem.getFdTable().stream().filter(fd1 -> fd1.getFdId() == registers[2]).toList().getFirst();
                fileSystem.closeFile(fd);
                break;
            //write file
            case 6:
                fd = fileSystem.getFdTable().stream().filter(fd1 -> fd1.getFdId() == registers[2]).toList().getFirst();
                objects = readMemory(registers[3], registers[4]);
                fd.getFileNode().setContents(Arrays.toString(objects));
                break;
            // create file
            case 9:
                path = readString(registers[2]);
                name = readString(registers[3]);
                registers[1] = fileSystem.createFile(path, FileTreeNode.FileType.FILE, name) ? 1 : 0;
                break;
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

    private Object[] readMemory(int begin, int end) {
        Object[] objects = new Object[(end - begin)/4];
        for (int i = begin; i < end; i+=4) {
            objects[(i - begin)>>2] = memory.getOrDefault(i, 0);
        }
        return objects;
    }

    // 获取寄存器状态（调试用）
    public void printRegisters() {
        System.out.println("Register Status:");
        System.out.println(Arrays.toString(registers));
        System.out.println("\n");
    }

    private String readString(int address) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int i = memory.getOrDefault(address, 0);
            char c = (char) i;
            if (c == '\0' || i == 0) {
                break;
            }
            sb.append(c);
            address+=2;
        }
        return sb.toString();
    }

    public int getPc() {
        return pc;
    }
}
