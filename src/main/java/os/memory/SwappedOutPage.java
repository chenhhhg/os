package os.memory;

/**
 * 被换出的页面类
 * 包含被换出页面的相关信息：进程ID、虚拟页号、页面内容
 */
public class SwappedOutPage {
    /**
     * 进程ID
     */
    private int processId;
    
    /**
     * 虚拟页号
     */
    private int virtualPage;
    
    /**
     * 页面内容
     */
    private Object[] pageContents;

    /**
     * 构造函数
     * @param processId 进程ID
     * @param virtualPage 虚拟页号
     * @param pageContents 页面内容
     */
    public SwappedOutPage(int processId, int virtualPage, Object[] pageContents) {
        this.processId = processId;
        this.virtualPage = virtualPage;
        this.pageContents = pageContents;
    }

    /**
     * 获取进程ID
     * @return 进程ID
     */
    public int getProcessId() {
        return processId;
    }

    /**
     * 设置进程ID
     * @param processId 进程ID
     */
    public void setProcessId(int processId) {
        this.processId = processId;
    }

    /**
     * 获取虚拟页号
     * @return 虚拟页号
     */
    public int getVirtualPage() {
        return virtualPage;
    }

    /**
     * 设置虚拟页号
     * @param virtualPage 虚拟页号
     */
    public void setVirtualPage(int virtualPage) {
        this.virtualPage = virtualPage;
    }

    /**
     * 获取页面内容
     * @return 页面内容
     */
    public Object[] getPageContents() {
        return pageContents;
    }

    /**
     * 设置页面内容
     * @param pageContents 页面内容
     */
    public void setPageContents(Object[] pageContents) {
        this.pageContents = pageContents;
    }
} 