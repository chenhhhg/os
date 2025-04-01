package os.memory;

/**
 * 页表项类
 * 包含页表项的基本信息：是否有效、是否被修改、对应的物理页号
 */
public class PageEntry {
    /**
     * 页是否有效（是否在物理内存中）
     */
    private boolean valid;
    
    /**
     * 页是否被修改过
     */
    private boolean dirty;
    
    /**
     * 对应的物理页号
     */
    private int physicalPage;

    /**
     * 构造函数
     * @param valid 是否有效
     * @param dirty 是否被修改
     * @param physicalPage 物理页号
     */
    public PageEntry(boolean valid, boolean dirty, int physicalPage) {
        this.valid = valid;
        this.dirty = dirty;
        this.physicalPage = physicalPage;
    }

    /**
     * 获取页是否有效
     * @return 页是否有效
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 设置页是否有效
     * @param valid 页是否有效
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * 获取页是否被修改
     * @return 页是否被修改
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * 设置页是否被修改
     * @param dirty 页是否被修改
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * 获取物理页号
     * @return 物理页号
     */
    public int getPhysicalPage() {
        return physicalPage;
    }

    /**
     * 设置物理页号
     * @param physicalPage 物理页号
     */
    public void setPhysicalPage(int physicalPage) {
        this.physicalPage = physicalPage;
    }

    @Override
    public String toString() {
        return "PageEntry{" +
                "valid=" + valid +
                ", dirty=" + dirty +
                ", physicalPage=" + physicalPage +
                '}';
    }
} 