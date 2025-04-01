package os.test.memory;

import os.constant.MemoryConstant;
import os.memory.MemoryManager;
import os.memory.PageEntry;
import os.memory.PageFaultHandler;
import os.process.PCB;
import os.process.ProcessState;
import os.system.SystemTimer;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 内存管理器测试类
 * 用于测试内存管理系统的功能
 */
public class MemoryManagerTest {

    private static MemoryManager memoryManager;
    private static Object[] memory;
    private static ArrayList<PCB> processes;
    
    public static void main(String[] args) {
        System.out.println("===== 内存管理测试程序 =====");
        
        // 初始化测试环境
        initializeTest();
        
        // 运行测试用例
        try {
            testBasicMemoryOperations();
            testPageFault();
            testMemoryUsage();
            testPageReplacementPolicy();
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("===== 测试完成 =====");
    }
    
    /**
     * 初始化测试环境
     */
    private static void initializeTest() {
        System.out.println("\n----- 初始化测试环境 -----");
        
        // 创建物理内存
        memory = new Object[MemoryConstant.PHYSICAL_MEMORY_SIZE];
        
        // 创建进程列表
        processes = new ArrayList<>();
        processes.add(new PCB(1, 0, 5)); // 创建进程，使用PCB(pid, priority, burstTime)构造函数
        processes.add(new PCB(2, 0, 5)); // 创建进程，使用PCB(pid, priority, burstTime)构造函数
        
        // 初始化内存管理器
        memoryManager = MemoryManager.getInstance();
        memoryManager.initialize(processes, memory, MemoryConstant.PAGE_SIZE, MemoryConstant.VIRTUAL_ADDRESS_SIZE);
        
        // 初始化页面错误处理器
        PageFaultHandler.initialize(memoryManager);
        
        // 初始化系统时钟 - 不需要显式初始化，SystemTimer类没有initialize方法
        SystemTimer.tick(); // 只需确保时钟开始计时
        
        System.out.println("初始化完成，内存大小: " + MemoryConstant.PHYSICAL_MEMORY_SIZE + " 字，页面大小: " + MemoryConstant.PAGE_SIZE + " 字");
    }
    
    /**
     * 测试基本内存操作
     * @throws Exception 如果发生错误
     */
    private static void testBasicMemoryOperations() throws Exception {
        System.out.println("\n----- 测试基本内存操作 -----");
        
        PCB process = processes.get(0);
        int virtualAddress = 512;  // 位于第二个虚拟页面
        String testData = "测试数据";
        
        // 写入数据
        System.out.println("尝试写入数据到虚拟地址 " + virtualAddress);
        
        // 第一次写入可能会发生页面错误
        if (!memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick())) {
            System.out.println("发生页面错误，正在处理...");
            // 处理页面错误
            PageFaultHandler.handlePageFault(process, virtualAddress);
            
            // 再次尝试写入
            boolean writeSuccess = memoryManager.memoryWrite(process, virtualAddress, testData, SystemTimer.getCurrentTick());
            System.out.println("写入" + (writeSuccess ? "成功" : "失败"));
        } else {
            System.out.println("写入成功");
        }
        
        // 读取数据
        System.out.println("尝试读取虚拟地址 " + virtualAddress + " 的数据");
        Object readData = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
        
        System.out.println("读取结果: " + readData);
        System.out.println("数据验证: " + (testData.equals(readData) ? "正确" : "错误"));
    }
    
    /**
     * 测试页面错误
     * @throws Exception 如果发生错误
     */
    private static void testPageFault() throws Exception {
        System.out.println("\n----- 测试页面错误 -----");
        
        PCB process = processes.get(1);
        int virtualAddress = 1024;  // 位于第四个虚拟页面
        
        System.out.println("尝试读取未分配的虚拟地址 " + virtualAddress);
        Object data = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
        
        if (data == null && process.getState() == ProcessState.WAITING) {
            System.out.println("页面错误触发成功，进程状态已设置为等待");
            
            // 处理页面错误
            PageFaultHandler.handlePageFault(process, virtualAddress);
            System.out.println("页面错误处理完成，进程状态: " + process.getState());
            
            // 再次尝试读取
            data = memoryManager.memoryRead(process, virtualAddress, SystemTimer.getCurrentTick());
            System.out.println("读取结果: " + data + " (预期为初始值null或0)");
        } else {
            System.out.println("页面错误测试结果异常");
        }
    }
    
    /**
     * 测试内存使用情况
     */
    private static void testMemoryUsage() {
        System.out.println("\n----- 测试内存使用情况 -----");
        
        double usage = memoryManager.getMemoryUsage();
        System.out.println("当前内存使用率: " + String.format("%.2f%%", usage * 100));
        
        int[] pageBitmap = memoryManager.getPageBitmap();
        
        // 计算每个进程占用的页面数
        int[] pageCountPerProcess = new int[3];  // 0:未分配, 1:进程1, 2:进程2
        for (int pageOwner : pageBitmap) {
            if (pageOwner >= 0 && pageOwner < 3) {
                pageCountPerProcess[pageOwner]++;
            }
        }
        
        System.out.println("未分配页面数: " + pageCountPerProcess[0]);
        System.out.println("进程1占用页面数: " + pageCountPerProcess[1]);
        System.out.println("进程2占用页面数: " + pageCountPerProcess[2]);
    }
    
    /**
     * 测试页面替换策略
     * @throws Exception 如果发生错误
     */
    private static void testPageReplacementPolicy() throws Exception {
        System.out.println("\n----- 测试页面替换策略 -----");
        
        PCB process = processes.get(0);
        
        // 访问多个虚拟页面以触发页面替换
        System.out.println("连续访问多个不同的虚拟页面以触发页面替换");
        
        for (int i = 0; i < 10; i++) {
            int virtualAddress = i * MemoryConstant.PAGE_SIZE;
            System.out.println("访问虚拟地址: " + virtualAddress);
            
            try {
                if (!memoryManager.memoryWrite(process, virtualAddress, "测试数据" + i, SystemTimer.getCurrentTick())) {
                    PageFaultHandler.handlePageFault(process, virtualAddress);
                    memoryManager.memoryWrite(process, virtualAddress, "测试数据" + i, SystemTimer.getCurrentTick());
                }
                
                // 增加系统时钟
                SystemTimer.tick();
            } catch (Exception e) {
                System.err.println("访问虚拟地址 " + virtualAddress + " 时发生错误: " + e.getMessage());
            }
        }
        
        // 检查被换出的页面
        System.out.println("检查被换出的页面");
        ArrayList<os.memory.SwappedOutPage> swappedPages = memoryManager.getSwappedPages();
        System.out.println("被换出的页面数量: " + swappedPages.size());
    }
} 