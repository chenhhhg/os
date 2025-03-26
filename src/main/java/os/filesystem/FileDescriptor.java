package os.filesystem;

/**
 * 简化的文件描述符类
 */
public class FileDescriptor {
    // 拥有该文件描述符的进程ID
    private int pid;
    // 文件描述符ID
    private int fdId;
    // 指向文件节点的引用
    private FileTreeNode fileNode;
    // 文件读权限
    private boolean readable;
    // 文件写权限
    private boolean writable;
    // 文件游标位置
    private int cursor;
    
    public FileDescriptor(int pid, int fdId, FileTreeNode fileNode, boolean readable, boolean writable) {
        this.pid = pid;
        this.fdId = fdId;
        this.fileNode = fileNode;
        this.readable = readable;
        this.writable = writable;
        this.cursor = 0;
    }
    
    // Getters and Setters
    public int getPid() {
        return pid;
    }
    
    public int getFdId() {
        return fdId;
    }
    
    public FileTreeNode getFileNode() {
        return fileNode;
    }
    
    public boolean isReadable() {
        return readable;
    }
    
    public boolean isWritable() {
        return writable;
    }
    
    public int getCursor() {
        return cursor;
    }
    
    public void setCursor(int cursor) {
        this.cursor = cursor;
    }
    
    @Override
    public String toString() {
        return "FD{" +
                "id=" + fdId +
                ", file='" + (fileNode != null ? fileNode.getName() : "null") + '\'' +
                ", cursor=" + cursor +
                '}';
    }
} 