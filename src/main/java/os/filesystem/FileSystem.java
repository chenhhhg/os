package os.filesystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 简化的文件系统类，使用Java IO实现真实文件读写
 */
public class FileSystem implements FileOperations {
    
    // 单例模式
    private static final FileSystem INSTANCE = new FileSystem();
    
    // 文件系统根节点
    private final FileTreeNode root;
    
    // 文件描述符表
    private final List<FileDescriptor> fdTable;
    
    // 文件描述符计数器
    private int fdCounter;
    
    // 真实文件系统根目录
    private final String realFsRoot;
    
    /**
     * 私有构造方法，初始化文件系统
     */
    private FileSystem() {
        // 创建根目录
        root = new FileTreeNode(FileTreeNode.FileType.DIRECTORY, "/");
        root.setPath("/");
        
        // 初始化文件描述符表
        fdTable = new ArrayList<>();
        fdCounter = 0;
        
        // 设置真实文件系统的根目录
        realFsRoot = System.getProperty("user.dir") + File.separator + "virtualfs";
        
        // 确保真实文件系统根目录存在
        try {
            Files.createDirectories(Paths.get(realFsRoot));
        } catch (IOException e) {
            System.err.println("[文件系统错误] 无法创建虚拟文件系统根目录: " + e.getMessage());
        }
        
        // 创建基本目录结构
        initializeFileSystem();
    }
    
    /**
     * 获取文件系统实例
     */
    public static FileSystem getInstance() {
        return INSTANCE;
    }
    
    /**
     * 初始化文件系统基本结构
     */
    private void initializeFileSystem() {
        // 创建基本目录
        createFile("/", FileTreeNode.FileType.DIRECTORY, "bin");
        createFile("/", FileTreeNode.FileType.DIRECTORY, "etc");
        createFile("/", FileTreeNode.FileType.DIRECTORY, "home");
        createFile("/", FileTreeNode.FileType.DIRECTORY, "tmp");
        
        // 创建示例文件
        createFile("/home", FileTreeNode.FileType.DIRECTORY, "user");
        createFile("/home/user", FileTreeNode.FileType.FILE, "welcome.txt");
        
        // 设置welcome.txt内容
        FileTreeNode welcomeFile = findNode("/home/user/welcome.txt");
        if (welcomeFile != null) {
            try {
                writeToRealFile(getRealPath(welcomeFile.getPath()), "欢迎使用操作系统模拟器！\n这是一个简单的文件系统实现。", false);
                welcomeFile.setContents("欢迎使用操作系统模拟器！\n这是一个简单的文件系统实现。");
            } catch (IOException e) {
                System.err.println("[文件系统错误] 无法写入欢迎文件: " + e.getMessage());
            }
        }
        
        System.out.println("[系统] 文件系统初始化完成");
    }
    
    /**
     * 获取虚拟路径对应的真实文件系统路径
     */
    private String getRealPath(String virtualPath) {
        // 移除前导斜杠并替换所有斜杠为系统分隔符
        String relativePath = virtualPath.startsWith("/") ? 
                              virtualPath.substring(1).replace("/", File.separator) : 
                              virtualPath.replace("/", File.separator);
        return realFsRoot + File.separator + relativePath;
    }
    
    /**
     * 写入内容到真实文件
     */
    private void writeToRealFile(String realPath, String content, boolean append) throws IOException {
        // 确保父目录存在
        Path path = Paths.get(realPath);
        Files.createDirectories(path.getParent());
        
        // 写入文件内容
        if (append) {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.write(path, content.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    /**
     * 从真实文件读取内容
     */
    private String readFromRealFile(String realPath, int offset, int size) throws IOException {
        Path path = Paths.get(realPath);
        if (!Files.exists(path)) {
            return "";
        }
        
        byte[] content = Files.readAllBytes(path);
        String fileContent = new String(content, StandardCharsets.UTF_8);
        
        // 处理偏移和大小
        int endIndex = Math.min(offset + size, fileContent.length());
        if (offset >= fileContent.length()) {
            return "";
        }
        
        return fileContent.substring(offset, endIndex);
    }
    
    /**
     * 解析路径，返回路径组件数组
     */
    private String[] parsePath(String path) {
        // 处理路径分隔符，移除空组件
        List<String> components = new ArrayList<>();
        for (String component : path.split("/")) {
            if (!component.isEmpty()) {
                components.add(component);
            }
        }
        return components.toArray(new String[0]);
    }
    
    @Override
    public FileTreeNode findNode(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // 处理根路径
        if (path.equals("/")) {
            return root;
        }
        
        // 解析路径
        String[] components = parsePath(path);
        
        // 从根节点开始查找
        FileTreeNode current = root;
        for (String component : components) {
            if (current.getType() != FileTreeNode.FileType.DIRECTORY) {
                return null; // 路径中非目录部分不能有子节点
            }
            
            FileTreeNode next = current.findChild(component);
            if (next == null) {
                return null; // 找不到对应的子节点
            }
            current = next;
        }
        
        // 处理符号链接
        if (current.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
            return findNode(current.getLinkTarget());
        }
        
        return current;
    }
    
    @Override
    public boolean createFile(String parentPath, FileTreeNode.FileType type, String name) {
        // 查找父目录
        FileTreeNode parent = findNode(parentPath);
        if (parent == null || parent.getType() != FileTreeNode.FileType.DIRECTORY) {
            System.out.println("[文件系统] 创建失败：父目录不存在或不是目录 - " + parentPath);
            return false;
        }
        
        // 检查是否已存在同名文件
        if (parent.findChild(name) != null) {
            System.out.println("[文件系统] 创建失败：同名文件已存在 - " + name);
            return false;
        }
        
        // 创建新节点
        FileTreeNode newNode = new FileTreeNode(type, name);
        
        // 设置完整路径
        String fullPath = parentPath;
        if (!parentPath.endsWith("/")) {
            fullPath += "/";
        }
        fullPath += name;
        newNode.setPath(fullPath);
        
        // 添加到父节点
        parent.addChild(newNode);
        
        // 在真实文件系统中创建对应的文件或目录
        try {
            String realPath = getRealPath(fullPath);
            if (type == FileTreeNode.FileType.DIRECTORY) {
                Files.createDirectories(Paths.get(realPath));
            } else if (type == FileTreeNode.FileType.FILE) {
                Files.createDirectories(Paths.get(realPath).getParent());
                Files.createFile(Paths.get(realPath));
            } else if (type == FileTreeNode.FileType.SYMBOLIC_LINK) {
                // 符号链接将在创建时设置目标
            }
        } catch (IOException e) {
            System.err.println("[文件系统错误] 创建文件/目录失败: " + e.getMessage());
            // 如果真实文件创建失败，从树中移除节点
            parent.removeChild(newNode);
            return false;
        }
        
        System.out.println("[文件系统] 创建" + type + "成功：" + fullPath);
        return true;
    }
    
    @Override
    public boolean deleteFile(String path) {
        // 不能删除根目录
        if (path.equals("/")) {
            System.out.println("[文件系统] 删除失败：不能删除根目录");
            return false;
        }
        
        // 解析路径
        String[] components = parsePath(path);
        if (components.length == 0) {
            return false;
        }
        
        // 获取父目录路径和文件名
        String fileName = components[components.length - 1];
        String parentPath = "/";
        if (components.length > 1) {
            parentPath = "/" + String.join("/", Arrays.copyOfRange(components, 0, components.length - 1));
        }
        
        // 查找父目录
        FileTreeNode parent = findNode(parentPath);
        if (parent == null || parent.getType() != FileTreeNode.FileType.DIRECTORY) {
            System.out.println("[文件系统] 删除失败：父目录不存在或不是目录 - " + parentPath);
            return false;
        }
        
        // 查找要删除的节点
        FileTreeNode nodeToDelete = parent.findChild(fileName);
        if (nodeToDelete == null) {
            System.out.println("[文件系统] 删除失败：文件不存在 - " + path);
            return false;
        }
        
        // 检查文件是否被打开
        if (nodeToDelete.getOpenFileDescriptor() != null) {
            System.out.println("[文件系统] 删除失败：文件已被打开 - " + path);
            return false;
        }
        
        // 如果是目录，检查是否为空
        if (nodeToDelete.getType() == FileTreeNode.FileType.DIRECTORY && !nodeToDelete.getChildren().isEmpty()) {
            System.out.println("[文件系统] 删除失败：目录不为空 - " + path);
            return false;
        }
        
        // 在真实文件系统中删除文件或目录
        try {
            String realPath = getRealPath(path);
            if (nodeToDelete.getType() == FileTreeNode.FileType.DIRECTORY) {
                Files.delete(Paths.get(realPath));
            } else {
                Files.deleteIfExists(Paths.get(realPath));
            }
        } catch (IOException e) {
            System.err.println("[文件系统错误] 删除文件/目录失败: " + e.getMessage());
            return false;
        }
        
        // 执行删除
        boolean result = parent.removeChild(nodeToDelete);
        
        if (result) {
            System.out.println("[文件系统] 删除成功：" + path);
        }
        
        return result;
    }
    
    @Override
    public FileDescriptor openFile(String path, int pid, boolean readable, boolean writable) {
        // 查找文件
        FileTreeNode node = findNode(path);
        if (node == null) {
            System.out.println("[文件系统] 打开失败：文件不存在 - " + path);
            return null;
        }
        
        // 不能直接打开目录
        if (node.getType() == FileTreeNode.FileType.DIRECTORY) {
            System.out.println("[文件系统] 打开失败：不能直接打开目录 - " + path);
            return null;
        }
        
        // 检查文件是否已被打开
        if (node.getOpenFileDescriptor() != null) {
            System.out.println("[文件系统] 打开失败：文件已被打开 - " + path);
            return null;
        }
        
        // 如果是符号链接，检查目标文件是否存在
        if (node.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
            String targetPath = node.getLinkTarget();
            FileTreeNode targetNode = findNode(targetPath);
            if (targetNode == null || targetNode.getType() != FileTreeNode.FileType.FILE) {
                System.out.println("[文件系统] 打开失败：符号链接目标不存在或不是文件 - " + targetPath);
                return null;
            }
        }
        
        // 创建文件描述符
        fdCounter++;
        FileDescriptor fd = new FileDescriptor(pid, fdCounter, node, readable, writable);
        
        // 尝试在真实文件系统中打开文件
        try {
            String realPath = getRealPath(path);
            Path filePath = Paths.get(realPath);
            
            // 确保文件存在
            if (!Files.exists(filePath) && node.getType() == FileTreeNode.FileType.FILE) {
                Files.createFile(filePath);
            }
            
            // 读取文件内容到内存
            if (node.getType() == FileTreeNode.FileType.FILE) {
                String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                node.setContents(content);
            } 
            // 如果是符号链接，读取目标文件的内容
            else if (node.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
                String targetPath = node.getLinkTarget();
                String realTargetPath = getRealPath(targetPath);
                if (Files.exists(Paths.get(realTargetPath))) {
                    FileTreeNode targetNode = findNode(targetPath);
                    if (targetNode != null && targetNode.getType() == FileTreeNode.FileType.FILE) {
                        // 目标文件是存在的，不需要额外操作
                    } else {
                        System.out.println("[文件系统] 打开失败：符号链接目标不是有效文件 - " + targetPath);
                        return null;
                    }
                } else {
                    System.out.println("[文件系统] 打开失败：符号链接目标不存在 - " + targetPath);
                    return null;
                }
            }
        } catch (IOException e) {
            System.err.println("[文件系统错误] 打开文件失败: " + e.getMessage());
            return null;
        }
        
        // 更新节点和文件描述符表
        node.setOpenFileDescriptor(fd);
        fdTable.add(fd);
        
        System.out.println("[文件系统] 打开成功：" + path + " (FD: " + fdCounter + ")");
        return fd;
    }
    
    @Override
    public boolean closeFile(FileDescriptor fd) {
        if (fd == null) {
            return false;
        }
        
        FileTreeNode node = fd.getFileNode();
        if (node == null) {
            return false;
        }
        
        // 如果文件已修改，将内容写入真实文件系统
        try {
            if (node.getType() == FileTreeNode.FileType.FILE) {
                String content = node.getContents();
                writeToRealFile(getRealPath(node.getPath()), content, false);
            }
        } catch (IOException e) {
            System.err.println("[文件系统错误] 关闭文件时写入失败: " + e.getMessage());
        }
        
        // 清除文件描述符关联
        node.setOpenFileDescriptor(null);
        
        // 从文件描述符表中移除
        boolean result = fdTable.remove(fd);
        
        if (result) {
            System.out.println("[文件系统] 关闭文件成功：" + node.getPath() + " (FD: " + fd.getFdId() + ")");
        }
        
        return result;
    }
    
    @Override
    public String readFile(FileDescriptor fd, int size) {
        if (fd == null || !fd.isReadable()) {
            return null;
        }
        
        FileTreeNode node = fd.getFileNode();
        if (node == null) {
            return null;
        }
        
        // 对于普通文件，从内存中读取
        if (node.getType() == FileTreeNode.FileType.FILE) {
            String content = node.getContents();
            int cursor = fd.getCursor();
            
            // 检查光标位置
            if (cursor >= content.length()) {
                return "";
            }
            
            // 计算实际读取大小
            int endPos = Math.min(cursor + size, content.length());
            String result = content.substring(cursor, endPos);
            
            // 更新光标位置
            fd.setCursor(endPos);
            
            return result;
        } 
        // 对于符号链接，读取目标文件
        else if (node.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
            String targetPath = node.getLinkTarget();
            FileTreeNode targetNode = findNode(targetPath);
            
            if (targetNode == null || targetNode.getType() != FileTreeNode.FileType.FILE) {
                return null;
            }
            
            try {
                String realPath = getRealPath(targetPath);
                String content = readFromRealFile(realPath, fd.getCursor(), size);
                
                // 更新光标位置
                fd.setCursor(fd.getCursor() + content.length());
                
                return content;
            } catch (IOException e) {
                System.err.println("[文件系统错误] 读取符号链接目标失败: " + e.getMessage());
                return null;
            }
        } 
        
        return null;
    }
    
    @Override
    public boolean writeFile(FileDescriptor fd, String content, boolean append) {
        if (fd == null || !fd.isWritable() || content == null) {
            return false;
        }
        
        FileTreeNode node = fd.getFileNode();
        if (node == null) {
            return false;
        }
        
        try {
            // 对于普通文件
            if (node.getType() == FileTreeNode.FileType.FILE) {
                String originalContent = node.getContents();
                String newContent;
                
                if (append) {
                    // 追加模式 - 始终追加到文件末尾
                    newContent = originalContent + content;
                    node.setContents(newContent);
                    fd.setCursor(newContent.length());
                    
                    // 写入真实文件
                    writeToRealFile(getRealPath(node.getPath()), newContent, false);
                    
                    System.out.println("[文件系统] 追加内容到文件：" + node.getPath() + " 大小: " + content.length());
                } else {
                    // 覆盖模式（从当前光标位置开始）
                    int cursor = fd.getCursor();
                    if (cursor >= originalContent.length()) {
                        // 光标在文件末尾，直接追加
                        newContent = originalContent + content;
                    } else {
                        // 光标在文件中间，替换部分内容，但保留原始文件中光标后面的内容
                        newContent = originalContent.substring(0, cursor) + content;
                        if (cursor + content.length() < originalContent.length()) {
                            newContent += originalContent.substring(cursor + content.length());
                        }
                    }
                    
                    node.setContents(newContent);
                    fd.setCursor(cursor + content.length());
                    
                    // 写入真实文件
                    writeToRealFile(getRealPath(node.getPath()), newContent, false);
                    
                    System.out.println("[文件系统] 写入文件：" + node.getPath() + " 大小: " + content.length() + 
                                " 光标位置: " + cursor);
                }
                
                return true;
            } 
            // 对于符号链接，写入目标文件
            else if (node.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
                String targetPath = node.getLinkTarget();
                FileTreeNode targetNode = findNode(targetPath);
                
                if (targetNode == null || targetNode.getType() != FileTreeNode.FileType.FILE) {
                    System.out.println("[文件系统] 写入符号链接失败：目标不存在或不是文件 - " + targetPath);
                    return false;
                }
                
                // 更新目标文件内容
                String realPath = getRealPath(targetPath);
                String originalContent = targetNode.getContents();
                String newContent;
                
                if (append) {
                    // 追加模式
                    newContent = originalContent + content;
                    targetNode.setContents(newContent);
                    writeToRealFile(realPath, newContent, false);
                    
                    // 更新光标位置到文件末尾
                    fd.setCursor(newContent.length());
                    
                    System.out.println("[文件系统] 通过符号链接追加内容到文件：" + targetPath + " 大小: " + content.length());
                } else {
                    // 覆盖模式
                    int cursor = fd.getCursor();
                    if (cursor >= originalContent.length()) {
                        newContent = originalContent + content;
                    } else {
                        newContent = originalContent.substring(0, cursor) + content;
                        if (cursor + content.length() < originalContent.length()) {
                            newContent += originalContent.substring(cursor + content.length());
                        }
                    }
                    
                    targetNode.setContents(newContent);
                    writeToRealFile(realPath, newContent, false);
                    
                    // 更新光标位置
                    fd.setCursor(cursor + content.length());
                    
                    System.out.println("[文件系统] 通过符号链接写入文件：" + targetPath + " 大小: " + content.length() + 
                                " 光标位置: " + cursor);
                }
                
                return true;
            }
            
            return false;
        } catch (IOException e) {
            System.err.println("[文件系统错误] 写入文件失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean createSymbolicLink(String targetPath, String linkPath) {
        // 查找目标文件
        FileTreeNode targetNode = findNode(targetPath);
        if (targetNode == null) {
            System.out.println("[文件系统] 创建符号链接失败：目标不存在 - " + targetPath);
            return false;
        }
        
        // 解析链接路径
        String[] components = parsePath(linkPath);
        if (components.length == 0) {
            return false;
        }
        
        // 获取父目录路径和链接名
        String linkName = components[components.length - 1];
        String parentPath = "/";
        if (components.length > 1) {
            parentPath = "/" + String.join("/", Arrays.copyOfRange(components, 0, components.length - 1));
        }
        
        // 检查父目录是否存在
        FileTreeNode parentNode = findNode(parentPath);
        if (parentNode == null || parentNode.getType() != FileTreeNode.FileType.DIRECTORY) {
            System.out.println("[文件系统] 创建符号链接失败：父目录不存在 - " + parentPath);
            return false;
        }
        
        // 检查是否已存在同名文件
        if (parentNode.findChild(linkName) != null) {
            System.out.println("[文件系统] 创建符号链接失败：同名文件已存在 - " + linkPath);
            return false;
        }
        
        // 直接创建符号链接节点，不使用createFile
        FileTreeNode linkNode = new FileTreeNode(FileTreeNode.FileType.SYMBOLIC_LINK, linkName);
        linkNode.setPath(linkPath);
        linkNode.setLinkTarget(targetPath);
        parentNode.addChild(linkNode);
        
        // 在真实文件系统中创建符号链接（模拟方式）
        try {
            String realLinkPath = getRealPath(linkPath);
            
            // 确保父目录存在
            Files.createDirectories(Paths.get(realLinkPath).getParent());
            
            // 使用文本文件模拟符号链接
            // 写入特殊标记和目标路径到文件
            String symLinkContent = "SYMLINK:" + targetPath;
            Files.write(Paths.get(realLinkPath), symLinkContent.getBytes(StandardCharsets.UTF_8));
            
            System.out.println("[文件系统] 创建符号链接成功：" + linkPath + " -> " + targetPath);
            return true;
        } catch (IOException e) {
            System.err.println("[文件系统错误] 创建符号链接失败: " + e.getMessage());
            // 如果真实符号链接创建失败，从虚拟文件系统中删除
            parentNode.removeChild(linkNode);
            return false;
        }
    }
    
    @Override
    public String[] listDirectory(String path) {
        // 查找目录
        FileTreeNode node = findNode(path);
        if (node == null || node.getType() != FileTreeNode.FileType.DIRECTORY) {
            System.out.println("[文件系统] 列目录失败：路径不存在或不是目录 - " + path);
            return new String[0];
        }
        
        // 获取子节点列表
        List<FileTreeNode> children = node.getChildren();
        String[] result = new String[children.size()];
        
        for (int i = 0; i < children.size(); i++) {
            FileTreeNode child = children.get(i);
            StringBuilder sb = new StringBuilder();
            
            // 添加文件类型标识
            switch (child.getType()) {
                case DIRECTORY:
                    sb.append("d ");
                    break;
                case SYMBOLIC_LINK:
                    sb.append("l ");
                    break;
                default:
                    sb.append("- ");
                    break;
            }
            
            // 添加文件名
            sb.append(child.getName());
            
            // 对于符号链接，添加链接目标
            if (child.getType() == FileTreeNode.FileType.SYMBOLIC_LINK) {
                sb.append(" -> ").append(child.getLinkTarget());
            }
            
            result[i] = sb.toString();
        }
        
        return result;
    }
    
    public List<FileDescriptor> getFdTable() {
        return fdTable;
    }
    
    public FileTreeNode getRoot() {
        return root;
    }
} 