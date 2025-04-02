package os.constant;

/**
 * 内存管理常量类
 * 定义内存管理相关的常量
 */
public class MemoryConstant {
    /**
     * 页面大小（单位：字）
     */
    public static final int PAGE_SIZE = 256;
    
    /**
     * 物理内存大小（单位：字）
     */
    public static final int PHYSICAL_MEMORY_SIZE = 4096;
    
    /**
     * 虚拟地址空间大小（单位：字）
     */
    public static final int VIRTUAL_ADDRESS_SIZE = 4096;
    
    /**
     * 页表数量上限
     */
    public static final int MAX_PAGE_TABLE_COUNT = 8;
    
    /**
     * 页表项在内存中的起始位置
     */
    public static final int PAGE_TABLE_START = 0;
    
    /**
     * 控制寄存器在PCB中的索引位置
     */
    public static final int CONTROL_REGISTER_INDEX = 15;
} 