package os.memory;

import os.process.PCB;
import os.system.SystemTimer;

/**
 * 页面故障处理器
 * 用于处理页面故障
 */
public class PageFaultHandler {
    
    /**
     * 内存管理器
     */
    private static MemoryManager memoryManager;
    
    /**
     * 初始化页面故障处理器
     * @param memoryManager 内存管理器
     */
    public static void initialize(MemoryManager manager) {
        memoryManager = manager;
    }
    
    /**
     * 处理页面故障
     * @param process 进程
     * @param virtualAddress 虚拟地址
     * @throws Exception 如果处理过程中发生错误
     */
    public static void handlePageFault(PCB process, int virtualAddress) throws Exception {
        System.out.println("[页面故障] 进程ID: " + process.getPid() + ", 虚拟地址: " + virtualAddress);
        
        // 调用内存管理器的页面重入功能
        memoryManager.pageReenter(process, virtualAddress, SystemTimer.getCurrentTick());
        
        System.out.println("[页面重入] 完成");
    }
} 