package os.filesystem;

/**
 * 简化的文件操作接口
 */
public interface FileOperations {
    
    /**
     * 创建文件或目录
     * @param path 父目录路径
     * @param type 文件类型
     * @param name 文件名称
     * @return 成功返回true，失败返回false
     */
    boolean createFile(String path, FileTreeNode.FileType type, String name);
    
    /**
     * 删除文件或目录
     * @param path 文件路径
     * @return 成功返回true，失败返回false
     */
    boolean deleteFile(String path);
    
    /**
     * 打开文件
     * @param path 文件路径
     * @param pid 进程ID
     * @param readable 是否可读
     * @param writable 是否可写
     * @return 文件描述符，打开失败返回null
     */
    FileDescriptor openFile(String path, int pid, boolean readable, boolean writable);
    
    /**
     * 关闭文件
     * @param fd 文件描述符
     * @return 成功返回true，失败返回false
     */
    boolean closeFile(FileDescriptor fd);
    
    /**
     * 读取文件内容
     * @param fd 文件描述符
     * @param size 读取大小
     * @return 读取的内容
     */
    String readFile(FileDescriptor fd, int size);
    
    /**
     * 写入文件内容
     * @param fd 文件描述符
     * @param content 写入的内容
     * @param append 是否追加模式
     * @return 成功返回true，失败返回false
     */
    boolean writeFile(FileDescriptor fd, String content, boolean append);
    
    /**
     * 创建符号链接
     * @param targetPath 目标路径
     * @param linkPath 链接路径
     * @return 成功返回true，失败返回false
     */
    boolean createSymbolicLink(String targetPath, String linkPath);
    
    /**
     * 列出目录内容
     * @param path 目录路径
     * @return 目录内容的字符串数组
     */
    String[] listDirectory(String path);
    
    /**
     * 查找文件节点
     * @param path 文件路径
     * @return 找到的文件节点，未找到返回null
     */
    FileTreeNode findNode(String path);
} 