package os.memory;

import os.constant.MemoryConstant;
import os.process.PCB;
import os.process.ProcessState;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 内存管理器实现类
 * 实现内存管理单元的核心功能
 */
public class MemoryManager {
    
    private static final MemoryManager INSTANCE = new MemoryManager();
    
    /**
     * 获取内存管理器实例
     * @return 内存管理器实例
     */
    public static MemoryManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 物理内存
     */
    private Object[] memory;
    
    /**
     * 进程列表
     */
    private ArrayList<PCB> processes;
    
    /**
     * 页面位图（记录每个物理页面属于哪个进程ID，0表示未分配）
     */
    private int[] pageBitmap;
    
    /**
     * 页面最后访问时间
     */
    private int[] pageLastVisit;
    
    /**
     * 页面大小
     */
    private int pageSize;
    
    /**
     * 页表大小（虚拟页面数量）
     */
    private int pageTableSize;
    
    /**
     * 物理页面数量
     */
    private int physicalPageCount;
    
    /**
     * 页表在内存中的起始位置
     */
    private int pageTableStart;
    
    /**
     * 被换出的页面列表
     */
    private ArrayList<SwappedOutPage> swappedPages;
    
    /**
     * 私有构造函数
     */
    private MemoryManager() {
        this.swappedPages = new ArrayList<>();
    }
    
    public void initialize(ArrayList<PCB> tasks, Object[] memory, int pageSize, int virtualAddressSize) {
        this.memory = memory;
        this.processes = tasks;
        this.pageSize = pageSize;
        this.pageTableSize = virtualAddressSize / pageSize;
        this.physicalPageCount = memory.length / pageSize;
        this.pageBitmap = new int[physicalPageCount];
        this.pageLastVisit = new int[physicalPageCount];
        this.pageTableStart = MemoryConstant.PAGE_TABLE_START;
        this.swappedPages = new ArrayList<>();
        
        System.out.printf("[系统页面大小]: %d 页 \n", physicalPageCount);
    }
    
    public boolean memoryWrite(PCB process, int virtualAddress, Object content, int cpuTick) throws Exception {
        int virtualPage = getPage(virtualAddress);
        int offset = getOffset(virtualAddress);
        PageEntry pageEntry = getPageEntry(process, virtualPage);
        
        if (pageEntry == null || !pageEntry.isValid() || 
            pageBitmap[pageEntry.getPhysicalPage()] != process.getPid()) {
            // 页面错误
            setPendingPageFault(process, virtualAddress);
            return false;
        } else {
            // 写入内存
            memory[pageEntry.getPhysicalPage() * pageSize + offset] = content;
            pageEntry.setDirty(true);
            pageLastVisit[pageEntry.getPhysicalPage()] = cpuTick;
            return true;
        }
    }
    
    public Object memoryRead(PCB process, int virtualAddress, int cpuTick) throws Exception {
        int virtualPage = getPage(virtualAddress);
        int offset = getOffset(virtualAddress);
        PageEntry pageEntry = getPageEntry(process, virtualPage);
        
        if (pageEntry == null || !pageEntry.isValid() || 
            pageBitmap[pageEntry.getPhysicalPage()] != process.getPid()) {
            // 页面错误
            setPendingPageFault(process, virtualAddress);
            return null;
        } else {
            // 读取内存
            pageLastVisit[pageEntry.getPhysicalPage()] = cpuTick;
            return memory[pageEntry.getPhysicalPage() * pageSize + offset];
        }
    }
    
    /**
     * 设置页面错误
     * @param process 进程
     * @param virtualAddress 虚拟地址
     */
    private void setPendingPageFault(PCB process, int virtualAddress) {
        // 设置进程状态为等待
        process.setState(ProcessState.WAITING);
        // 记录引起页面错误的虚拟地址
        process.setFaultVirtualAddress(virtualAddress);
        System.out.println("[页面错误] 进程ID: " + process.getPid() + ", 虚拟地址: " + virtualAddress);
    }
    
    public void pageReenter(PCB process, int virtualAddress, int cpuTick) throws Exception {
        int virtualPage = getPage(virtualAddress);
        int physicalPage = allocatePhysicalPage(process, virtualPage, cpuTick);
        Object[] contents = null;
        
        // 查找被换出的页面
        SwappedOutPage swappedPage = null;
        for (SwappedOutPage page : swappedPages) {
            if (page.getProcessId() == process.getPid() && page.getVirtualPage() == virtualPage) {
                contents = page.getPageContents();
                swappedPage = page;
                break;
            }
        }
        
        // 如果有被换出的页面，加载它；否则，创建一个空页面
        if (contents == null) {
            contents = new Object[pageSize];
        } else {
            swappedPages.remove(swappedPage);
        }
        
        // 页面换入
        for (int i = 0; i < pageSize; i++) {
            int realAddress = i + physicalPage * pageSize;
            if (contents[i] != null) {
                memory[realAddress] = contents[i];
            } else {
                memory[realAddress] = 0;
            }
        }
        
        // 创建新的页表项
        setPageEntry(process, virtualPage, new PageEntry(true, false, physicalPage));
        
        // 重置进程状态
        process.setState(ProcessState.READY);
    }
    
    public void clearPageTable(PCB process) {
        // 清除页表
        for (int i = 0; i < pageTableSize; i++) {
            int pageTableAddress = getPageTableBaseAddress(process) + i;
            if (pageTableAddress < memory.length) {
                memory[pageTableAddress] = null;
            }
        }
        
        // 释放物理页面
        for (int i = MemoryConstant.MAX_PAGE_TABLE_COUNT; i < physicalPageCount; i++) {
            if (pageBitmap[i] == process.getPid()) {
                pageBitmap[i] = 0;
            }
        }
    }
    
    public int[] getPageBitmap() {
        return pageBitmap;
    }
    
    public int[] getPageLastVisit() {
        return pageLastVisit;
    }
    
    public ArrayList<SwappedOutPage> getSwappedPages() {
        return swappedPages;
    }
    
    public int allocatePhysicalPage(PCB process, int virtualPageNumber, int cpuTick) throws Exception {
        // 首先寻找空闲页面
        for (int i = MemoryConstant.MAX_PAGE_TABLE_COUNT; i < physicalPageCount; i++) {
            if (pageBitmap[i] == 0) {
                pageBitmap[i] = process.getPid();
                pageLastVisit[i] = cpuTick;
                return i;
            }
        }
        
        // 如果没有空闲页面，使用LRU算法选择一个页面换出
        int swappedIndex = 0;
        int minVisitTime = Integer.MAX_VALUE;
        int swappedVirtualPage = 0;
        
        for (int i = MemoryConstant.MAX_PAGE_TABLE_COUNT; i < physicalPageCount; i++) {
            if (pageLastVisit[i] < minVisitTime) {
                minVisitTime = pageLastVisit[i];
                swappedIndex = i;
            }
        }
        
        // 保存被换出页面的内容
        Object[] contents = Arrays.copyOfRange(memory, 
                                             swappedIndex * pageSize, 
                                             (swappedIndex + 1) * pageSize);
        
        // 查找被换出页面所属的进程
        PCB targetProcess = null;
        for (PCB tempProcess : processes) {
            if (tempProcess.getPid() == pageBitmap[swappedIndex]) {
                targetProcess = tempProcess;
                break;
            }
        }
        
        if (targetProcess == null) {
            throw new Exception("无法找到进程，请检查换出页面的位图");
        }
        
        // 查找被换出的虚拟页面
        for (int i = 0; i < pageTableSize; i++) {
            int pageTableAddress = getPageTableBaseAddress(targetProcess) + i;
            if (pageTableAddress >= memory.length || memory[pageTableAddress] == null) {
                continue;
            }
            
            PageEntry entry = (PageEntry) memory[pageTableAddress];
            if (entry.getPhysicalPage() == swappedIndex && entry.isValid()) {
                entry.setValid(false);
                swappedVirtualPage = i;
                break;
            }
        }
        
        // 更新页面所有权
        pageBitmap[swappedIndex] = process.getPid();
        pageLastVisit[swappedIndex] = cpuTick;
        
        // 添加到被换出页面列表
        swappedPages.add(new SwappedOutPage(targetProcess.getPid(), swappedVirtualPage, contents));
        
        return swappedIndex;
    }
    
    public void freePhysicalPage(int physicalPageNumber) {
        if (physicalPageNumber >= 0 && physicalPageNumber < pageBitmap.length) {
            pageBitmap[physicalPageNumber] = 0;
        }
    }
    
    public PageEntry getPageEntry(PCB process, int virtualPageNumber) {
        int pageTableAddress = getPageTableBaseAddress(process) + virtualPageNumber;
        if (pageTableAddress >= memory.length || memory[pageTableAddress] == null) {
            return null;
        }
        return (PageEntry) memory[pageTableAddress];
    }
    
    public void setPageEntry(PCB process, int virtualPageNumber, PageEntry entry) {
        int pageTableAddress = getPageTableBaseAddress(process) + virtualPageNumber;
        if (pageTableAddress < memory.length) {
            memory[pageTableAddress] = entry;
        }
    }
    
    public int translateAddress(PCB process, int virtualAddress) throws Exception {
        int virtualPage = getPage(virtualAddress);
        int offset = getOffset(virtualAddress);
        PageEntry pageEntry = getPageEntry(process, virtualPage);
        
        if (pageEntry == null || !pageEntry.isValid()) {
            return -1;  // 地址转换失败
        }
        
        return pageEntry.getPhysicalPage() * pageSize + offset;
    }
    
    public double getMemoryUsage() {
        int usedCount = 0;
        for (Object obj : memory) {
            if (obj != null) {
                usedCount++;
            }
        }
        return (double) usedCount / memory.length;
    }
    
    /**
     * 获取页号
     * @param address 地址
     * @return 页号
     */
    private int getPage(int address) {
        return address / pageSize;
    }
    
    /**
     * 获取页内偏移
     * @param address 地址
     * @return 页内偏移
     */
    private int getOffset(int address) {
        return address % pageSize;
    }
    
    /**
     * 获取页表基址
     * @param process 进程
     * @return 页表基址
     */
    private int getPageTableBaseAddress(PCB process) {
        // 这里我们使用控制寄存器（在PCB寄存器数组中的最后一个位置）来存储页表基址
        // 如果控制寄存器为0，则分配一个新的页表
        int[] registers = process.getRegisters();
        if (registers[MemoryConstant.CONTROL_REGISTER_INDEX] == 0) {
            registers[MemoryConstant.CONTROL_REGISTER_INDEX] = pageTableStart + (process.getPid() * pageTableSize);
        }
        return registers[MemoryConstant.CONTROL_REGISTER_INDEX];
    }
} 