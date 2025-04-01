package os.test.memory;

import os.constant.MemoryConstant;
import os.cpu.CPU;
import os.memory.MemoryManager;
import os.memory.PageFaultHandler;
import os.process.PCB;
import os.process.ProcessController;
import os.process.ProcessState;
import os.system.SystemTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 内存管理系统测试主类
 * 这个类替代了MainWithMemory.java，提供了专门针对内存管理的测试功能
 */
public class MainMemoryTest {

    /**
     * 调试模式开关
     */
    public static boolean debug = false;
    
    /**
     * 内存管理器
     */
    private static MemoryManager memoryManager;
    
    /**
     * 物理内存
     */
    private static Object[] memory;
    
    /**
     * 进程列表
     */
    private static ArrayList<PCB> processes;

    public static void main(String[] args) {
        System.out.println("===== 内存管理系统测试 =====");
        
        // 初始化内存系统
        initializeMemorySystem();
        
        // 显示交互式菜单
        showMenu();
    }
    
    /**
     * 初始化内存系统
     */
    private static void initializeMemorySystem() {
        System.out.println("\n----- 初始化内存系统 -----");
        
        // 创建物理内存
        memory = new Object[MemoryConstant.PHYSICAL_MEMORY_SIZE];
        
        // 创建两个测试进程，使用PCB构造函数直接创建，避免读取资源文件
        processes = new ArrayList<>();
        processes.add(new PCB(1, 0, 10)); // 创建进程1，参数为(pid, priority, burstTime)
        processes.add(new PCB(2, 1, 10)); // 创建进程2，参数为(pid, priority, burstTime)
        
        // 初始化CPU
        CPU.getCPU().initialize(processes);
        
        // 初始化内存管理器
        memoryManager = MemoryManager.getInstance();
        memoryManager.initialize(processes, memory, MemoryConstant.PAGE_SIZE, MemoryConstant.VIRTUAL_ADDRESS_SIZE);
        
        // 初始化页面错误处理器
        PageFaultHandler.initialize(memoryManager);
        
        System.out.println("内存系统初始化完成");
        System.out.println("物理内存大小: " + MemoryConstant.PHYSICAL_MEMORY_SIZE + " 字");
        System.out.println("页面大小: " + MemoryConstant.PAGE_SIZE + " 字");
        System.out.println("虚拟地址空间大小: " + MemoryConstant.VIRTUAL_ADDRESS_SIZE + " 字");
    }
    
    /**
     * 显示交互式菜单
     */
    private static void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.println("\n----- 内存管理测试菜单 -----");
            System.out.println("1. 运行基本内存测试");
            System.out.println("2. 运行页面错误测试");
            System.out.println("3. 显示内存使用情况");
            System.out.println("4. 运行页面替换测试");
            System.out.println("5. 查看进程状态");
            System.out.println("6. 切换调试模式 (当前: " + (debug ? "开启" : "关闭") + ")");
            System.out.println("0. 退出");
            System.out.print("请选择操作: ");
            
            int choice = -1;
            try {
                choice = scanner.nextInt();
            } catch (Exception e) {
                scanner.nextLine(); // 清除输入缓冲
                System.out.println("输入无效，请重新选择");
                continue;
            }
            
            try {
                switch (choice) {
                    case 0:
                        running = false;
                        break;
                    case 1:
                        runBasicMemoryTest();
                        break;
                    case 2:
                        runPageFaultTest();
                        break;
                    case 3:
                        showMemoryUsage();
                        break;
                    case 4:
                        runPageReplacementTest();
                        break;
                    case 5:
                        showProcessStatus();
                        break;
                    case 6:
                        debug = !debug;
                        System.out.println("调试模式已" + (debug ? "开启" : "关闭"));
                        break;
                    default:
                        System.out.println("无效选择，请重新输入");
                }
            } catch (Exception e) {
                System.err.println("操作失败: " + e.getMessage());
                if (debug) {
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("程序已退出");
    }
    
    /**
     * 运行基本内存测试
     */
    private static void runBasicMemoryTest() throws Exception {
        System.out.println("\n----- 基本内存操作测试 -----");
        
        // 选择一个进程
        PCB process = processes.get(0);
        System.out.println("使用进程: PID=" + process.getPid());
        
        // 写入测试
        for (int i = 0; i < 3; i++) {
            int virtualAddress = i * MemoryConstant.PAGE_SIZE + 10;
            String testData = "测试数据-" + i;
            
            System.out.println("\n尝试写入数据 \"" + testData + "\" 到虚拟地址 " + virtualAddress);
            
            // 写入可能会失败并引发页面错误
            if (!memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick())) {
                System.out.println("发生页面错误，正在处理...");
                PageFaultHandler.handlePageFault(process, virtualAddress);
                
                // 再次尝试写入
                boolean writeSuccess = memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick());
                System.out.println("写入" + (writeSuccess ? "成功" : "失败"));
            } else {
                System.out.println("写入成功");
            }
            
            // 读取验证
            System.out.println("尝试读取虚拟地址 " + virtualAddress + " 的数据");
            Object readData = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
            System.out.println("读取结果: " + readData);
            System.out.println("数据验证: " + (testData.equals(readData) ? "正确" : "错误"));
            
            // 增加系统时钟
            SystemTimer.tick();
        }
    }
    
    /**
     * 运行页面错误测试
     */
    private static void runPageFaultTest() throws Exception {
        System.out.println("\n----- 页面错误测试 -----");
        
        // 使用另一个进程
        PCB process = processes.get(1);
        System.out.println("使用进程: PID=" + process.getPid());
        
        // 访问一个未分配的页面
        int virtualAddress = 3000;  // 确保这个地址在一个未分配的页面上
        System.out.println("尝试读取未分配的虚拟地址 " + virtualAddress);
        
        // 保存当前进程状态
        ProcessState originalState = process.getState();
        System.out.println("当前进程状态: " + originalState);
        
        // 尝试读取，预期会触发页面错误
        Object data = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
        
        // 检查是否发生页面错误
        if (data == null && process.getState() == ProcessState.WAITING) {
            System.out.println("页面错误已触发，进程状态已变为 " + process.getState());
            System.out.println("页面错误虚拟地址: " + process.getFaultVirtualAddress());
            
            // 处理页面错误
            System.out.println("正在处理页面错误...");
            PageFaultHandler.handlePageFault(process, virtualAddress);
            
            // 检查处理后的状态
            System.out.println("页面错误已处理，进程状态: " + process.getState());
            
            // 再次尝试读取
            data = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
            System.out.println("读取结果: " + data);
        } else {
            System.out.println("未触发页面错误或处理异常");
        }
    }
    
    /**
     * 显示内存使用情况
     */
    private static void showMemoryUsage() {
        System.out.println("\n----- 内存使用情况 -----");
        
        // 获取内存使用率
        double usage = memoryManager.getMemoryUsage();
        System.out.println("内存使用率: " + String.format("%.2f%%", usage * 100));
        
        // 获取页面位图
        int[] pageBitmap = memoryManager.getPageBitmap();
        int physicalPageCount = pageBitmap.length;
        
        // 统计页面分配情况
        int[] pageCountPerProcess = new int[processes.size() + 1]; // +1 用于未分配页面
        
        for (int i = 0; i < physicalPageCount; i++) {
            int owner = pageBitmap[i];
            if (owner >= 0 && owner <= processes.size()) {
                pageCountPerProcess[owner]++;
            }
        }
        
        System.out.println("物理页面总数: " + physicalPageCount);
        System.out.println("未分配页面数: " + pageCountPerProcess[0]);
        
        for (int i = 1; i < pageCountPerProcess.length; i++) {
            System.out.println("进程 " + i + " 占用页面数: " + pageCountPerProcess[i]);
        }
        
        // 显示被换出的页面
        ArrayList<os.memory.SwappedOutPage> swappedPages = memoryManager.getSwappedPages();
        System.out.println("被换出的页面数: " + swappedPages.size());
        
        // 详细显示被换出页面
        if (debug && !swappedPages.isEmpty()) {
            System.out.println("\n被换出的页面详情:");
            for (int i = 0; i < swappedPages.size(); i++) {
                os.memory.SwappedOutPage page = swappedPages.get(i);
                System.out.println("  页面 " + (i+1) + ": 进程ID=" + page.getProcessId() + 
                                   ", 虚拟页面=" + page.getVirtualPage());
            }
        }
    }
    
    /**
     * 运行页面替换测试
     */
    private static void runPageReplacementTest() throws Exception {
        System.out.println("\n----- 页面替换测试 -----");
        
        PCB process = processes.get(0);
        System.out.println("使用进程: PID=" + process.getPid());
        
        // 首先显示当前内存使用情况
        showMemoryUsage();
        
        System.out.println("\n开始连续访问不同的虚拟页面...");
        
        // 访问多个虚拟页面以触发页面替换
        int pageCount = 15; // 尝试访问超过物理页面数量的页面
        for (int i = 0; i < pageCount; i++) {
            int virtualAddress = i * MemoryConstant.PAGE_SIZE;
            String testData = "测试数据-页面" + i;
            
            System.out.println("\n访问虚拟地址: " + virtualAddress + ", 写入数据: " + testData);
            
            try {
                if (!memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick())) {
                    System.out.println("发生页面错误，正在处理...");
                    PageFaultHandler.handlePageFault(process, virtualAddress);
                    
                    boolean writeSuccess = memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick());
                    System.out.println("写入" + (writeSuccess ? "成功" : "失败"));
                } else {
                    System.out.println("写入成功");
                }
                
                // 增加系统时钟
                SystemTimer.tick();
            } catch (Exception e) {
                System.err.println("访问虚拟地址 " + virtualAddress + " 时发生错误: " + e.getMessage());
                if (debug) {
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("\n页面访问完成，显示当前内存使用情况:");
        showMemoryUsage();
        
        // 再次访问前面的页面，验证页面替换
        System.out.println("\n现在尝试重新访问第1个页面:");
        int firstPageAddress = 0;
        try {
            Object data = memoryManager.memoryRead(process, firstPageAddress, SystemTimer.getCurrentTick());
            System.out.println("读取虚拟地址 " + firstPageAddress + " 的结果: " + data);
            
            if (data == null && process.getState() == ProcessState.WAITING) {
                System.out.println("页面已被换出，需要重新载入");
                PageFaultHandler.handlePageFault(process, firstPageAddress);
                
                data = memoryManager.memoryRead(process, firstPageAddress, SystemTimer.getCurrentTick());
                System.out.println("页面重入后的读取结果: " + data);
            }
        } catch (Exception e) {
            System.err.println("访问虚拟地址 " + firstPageAddress + " 时发生错误: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 显示进程状态
     */
    private static void showProcessStatus() {
        System.out.println("\n----- 进程状态 -----");
        
        for (PCB process : processes) {
            System.out.println("进程ID: " + process.getPid());
            System.out.println("  名称: " + "测试进程" + process.getPid()); // 直接使用进程ID生成名称
            System.out.println("  状态: " + process.getState());
            System.out.println("  优先级: " + process.getPriority());
            
            if (process.getState() == ProcessState.WAITING) {
                System.out.println("  页面错误虚拟地址: " + process.getFaultVirtualAddress());
            }
            
            System.out.println();
        }
    }
} 