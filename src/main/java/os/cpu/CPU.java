package os.cpu;

import os.Main;
import os.constant.CPUConstant;
import os.filesystem.FileDescriptor;
import os.filesystem.FileSystem;
import os.filesystem.FileTreeNode;
import os.process.PCB;

import java.util.*;

import static os.constant.CPUConstant.REGISTER_COUNT;

/**
 * CPU模拟器类，实现一个简单的中央处理器
 * 采用单例模式设计，包含寄存器组、内存管理和指令执行功能
 */
public class CPU {
    // 单例实例
    private final static CPU INSTANCE = new CPU();
    // 寄存器组（通用寄存器）
    private final int[] registers = new int[REGISTER_COUNT];
    // 内存模拟，使用HashMap存储地址-值对（地址单位为字节）
    private final Map<Integer, Integer> memory = new HashMap<>();
    // 文件系统实例引用
    private final FileSystem fileSystem = FileSystem.getInstance();
    // 当前要执行的指令列表（每个指令已分割为字符串数组）
    private List<String[]> instructions = new ArrayList<>();
    // 程序计数器（指向下一条要执行的指令索引）
    private int pc;
    // 当前正在执行的进程控制块
    private PCB currentProcess;

    // 私有构造器实现单例模式
    private CPU() {
    }

    /**
     * 获取CPU单例实例
     *
     * @return CPU的唯一实例
     */
    public static CPU getCPU() {
        return INSTANCE;
    }

    /**
     * 加载新程序到CPU执行（上下文切换）
     * @param process 要加载的进程控制块
     */
    public void loadProgram(PCB process) {
        // 保存当前进程的上下文
        if (currentProcess != null) {
            currentProcess.setPc(pc);  // 保存程序计数器
            currentProcess.updateRegisters(registers);  // 保存寄存器状态
        }
        // 加载新进程上下文
        currentProcess = process;
        instructions = process.getInstructions();  // 获取进程指令集
        pc = process.getPc();  // 设置程序计数器
        changeRegisters(process);  // 恢复寄存器状态
    }

    /**
     * 切换寄存器状态到指定进程
     * @param process 目标进程
     */
    private void changeRegisters(PCB process) {
        // 将进程保存的寄存器值复制到CPU寄存器组
        for (int i = 0; i < REGISTER_COUNT; i++) {
            registers[i] = process.getRegisters()[i];
        }
    }

    /**
     * 执行当前指令
     */
    public void execute() {
        // 获取当前要执行的指令
        String[] parts = instructions.get(pc);

        // 调试模式：打印指令并添加延迟
        if (Main.debug) {
            System.out.println("Running instruction: " + Arrays.toString(parts));
            try {
                // 根据时钟频率计算延迟（模拟指令执行时间）
                Thread.sleep(2500 / CPUConstant.CLOCK_PER_TICK);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 指令分派（根据操作码选择处理函数）
        switch (parts[0].toLowerCase()) {
            case "mov" -> handleMov(parts);       // 数据传送指令
            case "syscall" -> handleSyscall(Integer.parseInt(parts[1])); // 系统调用
            case "add" -> handleAdd(parts);      // 加法指令
            case "bnez" -> handleBnez(parts);    // 条件跳转指令
            case "nop" -> {
            }                     // 空操作指令
            case "exit" -> {
            }                    // 进程退出
        }

        // 调试模式：打印寄存器状态
        if (Main.debug) {
            printRegisters();
        }

        pc++;  // 程序计数器递增（默认顺序执行）
    }

    /**
     * 处理条件跳转指令（BNEZ: Branch if Not Equal Zero）
     * @param parts 指令各部分 [bnez, 条件寄存器, 偏移量]
     */
    private void handleBnez(String[] parts) {
        String dest = parts[1];  // 条件判断寄存器
        String src = parts[2];    // 跳转偏移量

        int conditionValue = parseOperand(dest);  // 获取寄存器值
        if (conditionValue != 0) {
            // 修改程序计数器（相对跳转）
            pc += parseOperand(src);
        }
    }

    /**
     * 处理加法指令（ADD）
     * @param parts 指令各部分 [add, 目标位置, 操作数1, 操作数2]
     */
    private void handleAdd(String[] parts) {
        String dest = parts[1];  // 目标位置（寄存器或内存地址）
        String src1 = parts[2];  // 源操作数1
        String src2 = parts[3];  // 源操作数2

        // 解析操作数值
        int value1 = parseOperand(src1);
        int value2 = parseOperand(src2);
        int result = value1 + value2;

        // 根据目标位置类型存储结果
        if (dest.startsWith("*")) {
            // 内存地址模式（*开头）
            int address = Integer.parseInt(dest.substring(1));
            memory.put(address, result);
        } else if (dest.startsWith("R")) {
            // 寄存器模式（R开头）
            registers[Integer.parseInt(dest.substring(1))] = result;
        }
    }

    /**
     * 处理MOV指令（支持字符串传输）
     * @param parts 指令各部分 [mov, 目标位置, 源]
     */
    private void handleMov(String[] parts) {
        String dest = parts[1];
        String src = parts[2];

        // 处理字符串传输（逐个字符写入内存）
        if (src.matches("[a-zA-Z]+")) {
            int address = Integer.parseInt(dest.substring(1));
            for (char c : src.toCharArray()) {
                // 分解字符为多个MOV指令处理
                parts[2] = c + "";
                parts[1] = "*" + address;
                handleMovSingle(parts);
                address += 2;  // 每个字符占2字节（模拟16位字符）
            }
        } else {
            // 常规MOV指令处理
            handleMovSingle(parts);
        }
    }

    /**
     * 处理单个MOV指令
     * @param parts 指令各部分 [mov, 目标位置, 源]
     */
    private void handleMovSingle(String[] parts) {
        String dest = parts[1];
        String src = parts[2];

        int value = parseOperand(src);  // 解析源操作数值

        // 根据目标位置类型存储值
        if (dest.startsWith("*")) {
            // 内存地址模式（两种形式：*数字 或 *R寄存器）
            if (dest.substring(1).startsWith("R")) {
                // 寄存器间接寻址：地址来自寄存器值
                int address = registers[Integer.parseInt(dest.substring(2))];
                memory.put(address, value);
            } else {
                // 直接地址模式
                int address = Integer.parseInt(dest.substring(1));
                memory.put(address, value);
            }
        } else if (dest.startsWith("R")) {
            // 寄存器直接寻址
            registers[Integer.parseInt(dest.substring(1))] = value;
        }
    }

    /**
     * 解析操作数（支持寄存器、内存地址、字符和立即数）
     * @param operand 操作数字符串
     * @return 解析后的整数值
     */
    private int parseOperand(String operand) {
        if (operand.startsWith("*")) {
            // 内存地址值
            int address = Integer.parseInt(operand.substring(1));
            return memory.getOrDefault(address, 0);
        } else if (operand.startsWith("R")) {
            // 寄存器值
            return registers[Integer.parseInt(operand.substring(1))];
        } else if (operand.matches("[a-zA-Z]+")) {
            // 字符转ASCII码
            return operand.charAt(0);
        }
        // 立即数
        return Integer.parseInt(operand);
    }

    /**
     * 处理系统调用（模拟Linux syscall）
     * @param callNumber 系统调用号
     */
    private void handleSyscall(int callNumber) {
        String path;
        String name;
        FileDescriptor fd;
        Object[] objects;

        switch (callNumber) {
            case 3:  // 打开文件
                path = readString(registers[2]);  // 路径参数在R2
                name = readString(registers[3]);   // 文件名在R3
                fd = fileSystem.openFile(path + "/" + name, currentProcess.getPid(), true, true);
                registers[1] = fd.getFdId();  // 返回文件描述符ID到R1
                break;

            case 4:  // 关闭文件
                int fdId = registers[2];  // 文件描述符ID在R2
                fd = fileSystem.getFdTable().stream()
                        .filter(fd1 -> fd1.getFdId() == fdId)
                        .findFirst().orElse(null);
                if (fd != null) {
                    fileSystem.closeFile(fd);
                }
                break;

            case 6:  // 写文件
                int writeFdId = registers[2];  // 文件描述符在R2
                int dataAddr = registers[3];  // 数据地址在R3
                int dataLength = registers[4];  // 数据长度在R4
                fd = fileSystem.getFdTable().stream()
                        .filter(fd1 -> fd1.getFdId() == writeFdId)
                        .findFirst().orElse(null);
                if (fd != null) {
                    objects = readMemory(dataAddr, dataAddr + dataLength);
                    fd.getFileNode().setContents(Arrays.toString(objects));
                }
                break;

            case 9:  // 创建文件
                path = readString(registers[2]);  // 路径在R2
                name = readString(registers[3]);   // 文件名在R3
                boolean success = fileSystem.createFile(path, FileTreeNode.FileType.FILE, name);
                registers[1] = success ? 1 : 0;  // 返回操作结果到R1
                break;

            case 10:  // 调试打印内存内容（自定义系统调用）
                System.out.println(List.of(
                        memory.getOrDefault(24, 0),
                        memory.getOrDefault(2025, 0),
                        memory.getOrDefault(3026, 0),
                        memory.getOrDefault(4027, 0)
                ));
                break;

            default:
                System.out.println("Unsupported syscall: " + callNumber);
        }
    }

    /**
     * 从内存读取指定范围的数据
     * @param begin 起始地址
     * @param end 结束地址（不含）
     * @return 包含数据的对象数组
     */
    private Object[] readMemory(int begin, int end) {
        Object[] objects = new Object[(end - begin) / 4];  // 假设每个数据项占4字节
        for (int i = begin; i < end; i += 4) {
            objects[(i - begin) >> 2] = memory.getOrDefault(i, 0);
        }
        return objects;
    }

    /**
     * 打印寄存器状态（调试用）
     */
    public void printRegisters() {
        System.out.println("Register Status:");
        System.out.println(Arrays.toString(registers));
        System.out.println("\n");
    }

    /**
     * 从内存读取以null结尾的字符串
     * @param address 字符串起始地址
     * @return 读取到的字符串
     */
    private String readString(int address) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int charCode = memory.getOrDefault(address, 0);
            if (charCode == 0) break;  // 遇到空字符停止
            sb.append((char) charCode);
            address += 2;  // 每个字符占2字节
        }
        return sb.toString();
    }

    /**
     * 获取当前程序计数器值
     * @return pc值
     */
    public int getPc() {
        return pc;
    }
}