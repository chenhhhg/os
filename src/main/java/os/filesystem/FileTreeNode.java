package os.filesystem;

import java.util.ArrayList;
import java.util.List;

/**
 * 简化的文件树节点类
 */
public class FileTreeNode {
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        FILE,           // 普通文件
        DIRECTORY,      // 目录
        SYMBOLIC_LINK   // 符号链接
    }
    
    // 子节点列表
    private final List<FileTreeNode> children;
    // 节点类型
    private FileType type;
    // 节点名称
    private String name;
    // 节点路径
    private String path;
    // 文件内容
    private String contents;
    // 符号链接指向的路径（仅对符号链接有效）
    private String linkTarget;
    // 当前打开此文件的文件描述符
    private FileDescriptor openFileDescriptor;
    
    // 内容锁，用于同步访问
    private final Object contentLock = new Object();
    
    /**
     * 创建一个新的文件树节点
     */
    public FileTreeNode() {
        this.children = new ArrayList<>();
        this.contents = "";
        this.path = "";
    }
    
    /**
     * 创建一个指定类型和名称的文件树节点
     */
    public FileTreeNode(FileType type, String name) {
        this();
        this.type = type;
        this.name = name;
    }
    
    /**
     * 添加子节点
     */
    public void addChild(FileTreeNode child) {
        if (this.type != FileType.DIRECTORY) {
            throw new IllegalStateException("只有目录可以添加子节点");
        }
        this.children.add(child);
    }
    
    /**
     * 移除子节点
     */
    public boolean removeChild(FileTreeNode child) {
        return this.children.remove(child);
    }
    
    /**
     * 查找子节点
     */
    public FileTreeNode findChild(String name) {
        for (FileTreeNode child : children) {
            if (child.name.equals(name)) {
                return child;
            }
        }
        return null;
    }
    
    /**
     * 读取文件内容
     */
    public String getContents() {
        synchronized (contentLock) {
            return this.contents;
        }
    }
    
    /**
     * 写入文件内容（覆盖）
     */
    public void setContents(String content) {
        synchronized (contentLock) {
            this.contents = content;
        }
    }
    
    /**
     * 追加文件内容
     */
    public void appendContents(String content) {
        synchronized (contentLock) {
            this.contents += content;
        }
    }
    
    /**
     * 获取文件大小
     */
    public int getSize() {
        synchronized (contentLock) {
            return this.contents.length();
        }
    }
    
    // Getters and Setters
    
    public List<FileTreeNode> getChildren() {
        return children;
    }
    
    public FileType getType() {
        return type;
    }
    
    public void setType(FileType type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getLinkTarget() {
        return linkTarget;
    }
    
    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }
    
    public FileDescriptor getOpenFileDescriptor() {
        return openFileDescriptor;
    }
    
    public void setOpenFileDescriptor(FileDescriptor openFileDescriptor) {
        this.openFileDescriptor = openFileDescriptor;
    }
    
    @Override
    public String toString() {
        return "FileTreeNode{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", children=" + children.size() +
                '}';
    }
} 