package os.test.filesystem;

import os.filesystem.FileDescriptor;
import os.filesystem.FileSystem;
import os.filesystem.FileTreeNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 文件系统测试类
 */
public class FileSystemTest {
    
    // 进程ID（模拟）
    private static final int TEST_PID = 999;
    
    public static void main(String[] args) {
        System.out.println("========== 文件系统测试开始 ==========");
        
        // 测试基本文件操作
        basicFileOperationsTest();
        
        // 测试目录操作
        directoryOperationsTest();
        
        // 测试符号链接
        symbolicLinkTest();
        
        // 测试文件读写
        fileReadWriteTest();
        
        // 测试虚拟文件系统与真实文件系统的映射
        realFileSystemTest();
        
        System.out.println("========== 文件系统测试完成 ==========");
    }
    
    /**
     * 测试基本文件操作
     */
    public static void basicFileOperationsTest() {
        System.out.println("----------------------基本文件操作测试开始!----------------------");
        FileSystem fs = FileSystem.getInstance();
        
        // 创建测试文件
        String testFilePath = "/tmp/test.txt";
        boolean createResult = fs.createFile("/tmp", FileTreeNode.FileType.FILE, "test.txt");
        System.out.println("创建文件结果: " + createResult);
        
        // 检查文件是否存在
        FileTreeNode node = fs.findNode(testFilePath);
        System.out.println("文件存在: " + (node != null));
        
        // 打开文件
        FileDescriptor fd = fs.openFile(testFilePath, TEST_PID, true, true);
        System.out.println("文件打开: " + (fd != null ? "成功" : "失败"));
        
        // 写入内容
        if (fd != null) {
            String content = "Hello, FileSystem!";
            boolean writeResult = fs.writeFile(fd, content, false);
            System.out.println("写入文件: " + (writeResult ? "成功" : "失败"));
            
            // 关闭文件
            boolean closeResult = fs.closeFile(fd);
            System.out.println("关闭文件: " + (closeResult ? "成功" : "失败"));
            
            // 重新打开并读取内容
            fd = fs.openFile(testFilePath, TEST_PID, true, false);
            String readContent = fs.readFile(fd, 100);
            System.out.println("读取内容: " + readContent);
            System.out.println("内容验证: " + content.equals(readContent));
            fs.closeFile(fd);
        }
        
        // 删除文件
        boolean deleteResult = fs.deleteFile(testFilePath);
        System.out.println("删除文件: " + (deleteResult ? "成功" : "失败"));
        
        // 检查文件是否已删除
        node = fs.findNode(testFilePath);
        System.out.println("文件已删除: " + (node == null));
        
        System.out.println("----------------------基本文件操作测试完成!----------------------");
    }
    
    /**
     * 测试目录操作
     */
    public static void directoryOperationsTest() {
        System.out.println("----------------------目录操作测试开始!----------------------");
        FileSystem fs = FileSystem.getInstance();
        
        // 创建测试目录
        String testDirPath = "/tmp/testdir";
        boolean createDirResult = fs.createFile("/tmp", FileTreeNode.FileType.DIRECTORY, "testdir");
        System.out.println("创建目录: " + (createDirResult ? "成功" : "失败"));
        
        // 在目录中创建文件
        boolean createFileResult = fs.createFile(testDirPath, FileTreeNode.FileType.FILE, "file1.txt");
        System.out.println("在目录中创建文件: " + (createFileResult ? "成功" : "失败"));
        
        // 列出目录内容
        String[] dirContents = fs.listDirectory(testDirPath);
        System.out.println("目录内容数量: " + dirContents.length);
        for (String item : dirContents) {
            System.out.println("  " + item);
        }
        
        // 尝试删除非空目录（应当失败）
        boolean deleteNonEmptyDirResult = fs.deleteFile(testDirPath);
        System.out.println("删除非空目录: " + (deleteNonEmptyDirResult ? "成功（错误）" : "失败（预期）"));
        
        // 删除目录中的文件
        boolean deleteFileResult = fs.deleteFile(testDirPath + "/file1.txt");
        System.out.println("删除目录中的文件: " + (deleteFileResult ? "成功" : "失败"));
        
        // 删除空目录
        boolean deleteEmptyDirResult = fs.deleteFile(testDirPath);
        System.out.println("删除空目录: " + (deleteEmptyDirResult ? "成功" : "失败"));
        
        System.out.println("----------------------目录操作测试完成!----------------------");
    }
    
    /**
     * 测试符号链接
     */
    public static void symbolicLinkTest() {
        System.out.println("----------------------符号链接测试开始!----------------------");
        FileSystem fs = FileSystem.getInstance();
        
        // 创建测试文件
        String targetPath = "/tmp/target.txt";
        boolean createTargetResult = fs.createFile("/tmp", FileTreeNode.FileType.FILE, "target.txt");
        System.out.println("创建目标文件: " + (createTargetResult ? "成功" : "失败"));
        
        // 打开文件并写入内容
        FileDescriptor fd = fs.openFile(targetPath, TEST_PID, true, true);
        if (fd != null) {
            boolean writeResult = fs.writeFile(fd, "Target file content", false);
            System.out.println("写入目标文件: " + (writeResult ? "成功" : "失败"));
            fs.closeFile(fd);
        }
        
        // 创建符号链接
        String linkPath = "/tmp/link.txt";
        boolean createLinkResult = fs.createSymbolicLink(targetPath, linkPath);
        System.out.println("创建符号链接: " + (createLinkResult ? "成功" : "失败"));
        
        // 检查链接是否真正创建成功
        FileTreeNode linkNode = fs.findNode(linkPath);
        boolean linkExists = (linkNode != null);
        boolean isLinkType = linkNode != null && linkNode.getType() == FileTreeNode.FileType.SYMBOLIC_LINK;
        boolean hasTarget = linkNode != null && linkNode.getLinkTarget() != null && linkNode.getLinkTarget().equals(targetPath);
        
        System.out.println("链接节点存在: " + linkExists);
        if (linkExists) {
            System.out.println("链接类型: " + linkNode.getType());
            System.out.println("链接类型正确: " + isLinkType);
            System.out.println("链接目标: " + (linkNode.getLinkTarget() != null ? linkNode.getLinkTarget() : "null"));
            System.out.println("链接目标正确: " + hasTarget);
        }
        
        // 只有链接创建成功才继续测试
        if (linkExists && isLinkType && hasTarget) {
            // 通过符号链接读取内容
            fd = fs.openFile(linkPath, TEST_PID, true, false);
            if (fd != null) {
                String content = fs.readFile(fd, 100);
                System.out.println("通过符号链接读取内容: " + content);
                System.out.println("通过链接读取内容验证: " + "Target file content".equals(content));
                fs.closeFile(fd);
            } else {
                System.out.println("无法通过符号链接打开文件");
            }
            
            // 通过符号链接写入内容
            fd = fs.openFile(linkPath, TEST_PID, true, true);
            if (fd != null) {
                boolean writeResult = fs.writeFile(fd, "Modified through link", false);
                System.out.println("通过符号链接写入: " + (writeResult ? "成功" : "失败"));
                fs.closeFile(fd);
                
                // 检查原始文件内容是否被修改
                fd = fs.openFile(targetPath, TEST_PID, true, false);
                if (fd != null) {
                    String content = fs.readFile(fd, 100);
                    System.out.println("原始文件内容: " + content);
                    System.out.println("内容已通过链接修改: " + content.equals("Modified through link"));
                    fs.closeFile(fd);
                }
            } else {
                System.out.println("无法通过符号链接打开文件进行写入");
            }
        }
        
        // 删除符号链接和目标文件
        boolean deleteLinkResult = fs.deleteFile(linkPath);
        boolean deleteTargetResult = fs.deleteFile(targetPath);
        System.out.println("删除符号链接: " + (deleteLinkResult ? "成功" : "失败"));
        System.out.println("删除目标文件: " + (deleteTargetResult ? "成功" : "失败"));
        
        System.out.println("----------------------符号链接测试完成!----------------------");
    }
    
    /**
     * 测试文件读写操作
     */
    public static void fileReadWriteTest() {
        System.out.println("----------------------文件读写测试开始!----------------------");
        FileSystem fs = FileSystem.getInstance();
        
        // 创建测试文件
        String testFilePath = "/tmp/rwtest.txt";
        fs.createFile("/tmp", FileTreeNode.FileType.FILE, "rwtest.txt");
        
        // 打开文件以写入
        FileDescriptor fd = fs.openFile(testFilePath, TEST_PID, true, true);
        if (fd != null) {
            // 写入初始内容
            String initialContent = "Initial content\n";
            boolean writeResult1 = fs.writeFile(fd, initialContent, false);
            System.out.println("写入初始内容: " + (writeResult1 ? "成功" : "失败"));
            System.out.println("当前光标位置: " + fd.getCursor());
            
            // 设置光标位置并写入（插入）
            fd.setCursor(initialContent.length());
            String insertContent = "Inserted content\n";
            boolean writeResult3 = fs.writeFile(fd, insertContent, false);
            System.out.println("在光标位置写入: " + (writeResult3 ? "成功" : "失败"));
            System.out.println("当前光标位置: " + fd.getCursor());
            
            // 追加内容
            String appendContent = "Appended content\n";
            boolean writeResult2 = fs.writeFile(fd, appendContent, true);
            System.out.println("追加内容: " + (writeResult2 ? "成功" : "失败"));
            System.out.println("当前光标位置: " + fd.getCursor());
            
            // 关闭文件
            fs.closeFile(fd);
            
            // 重新打开并读取全部内容
            fd = fs.openFile(testFilePath, TEST_PID, true, false);
            if (fd != null) {
                String content = fs.readFile(fd, 1000);
                System.out.println("文件最终内容:");
                System.out.println("---BEGIN CONTENT---");
                System.out.println(content);
                System.out.println("---END CONTENT---");
                
                // 验证内容
                String expectedContent = initialContent + insertContent + appendContent;
                boolean contentCorrect = content.equals(expectedContent);
                System.out.println("内容验证: " + (contentCorrect ? "正确" : "不正确"));
                if (!contentCorrect) {
                    System.out.println("期望内容: [" + expectedContent + "]");
                    System.out.println("实际内容: [" + content + "]");
                    
                    // 转换为字节数组并显示字符编码，帮助调试
                    System.out.println("预期内容长度: " + expectedContent.length());
                    System.out.println("实际内容长度: " + content.length());
                    System.out.println("预期内容字节: " + Arrays.toString(expectedContent.getBytes()));
                    System.out.println("实际内容字节: " + Arrays.toString(content.getBytes()));
                }
                
                // 重置光标并分块读取
                fd.setCursor(0);
                System.out.println("分块读取:");
                String chunk1 = fs.readFile(fd, 10);
                System.out.println("块1 (10字节): [" + chunk1 + "]");
                System.out.println("当前光标位置: " + fd.getCursor());
                String chunk2 = fs.readFile(fd, 10);
                System.out.println("块2 (10字节): [" + chunk2 + "]");
                System.out.println("当前光标位置: " + fd.getCursor());
                
                fs.closeFile(fd);
            }
        }
        
        // 删除测试文件
        fs.deleteFile(testFilePath);
        
        System.out.println("----------------------文件读写测试完成!----------------------");
    }
    
    /**
     * 测试虚拟文件系统与真实文件系统的映射
     */
    public static void realFileSystemTest() {
        System.out.println("----------------------真实文件系统映射测试开始!----------------------");
        FileSystem fs = FileSystem.getInstance();
        
        // 创建虚拟文件
        String virtualFilePath = "/tmp/realtest.txt";
        fs.createFile("/tmp", FileTreeNode.FileType.FILE, "realtest.txt");
        
        // 写入内容
        FileDescriptor fd = fs.openFile(virtualFilePath, TEST_PID, true, true);
        if (fd != null) {
            fs.writeFile(fd, "Real file system test content", false);
            fs.closeFile(fd);
        }
        
        // 检查真实文件是否存在
        String userDir = System.getProperty("user.dir");
        Path realFilePath = Paths.get(userDir, "virtualfs", "tmp", "realtest.txt");
        boolean realFileExists = Files.exists(realFilePath);
        System.out.println("真实文件存在: " + realFileExists);
        
        if (realFileExists) {
            try {
                // 读取真实文件内容
                String realContent = new String(Files.readAllBytes(realFilePath));
                System.out.println("真实文件内容: " + realContent);
                
                // 直接修改真实文件
                String modifiedContent = "Modified directly in real file system";
                Files.write(realFilePath, modifiedContent.getBytes());
                System.out.println("直接修改真实文件");
                
                // 通过虚拟文件系统读取修改后的内容
                fd = fs.openFile(virtualFilePath, TEST_PID, true, false);
                if (fd != null) {
                    String virtualContent = fs.readFile(fd, 1000);
                    System.out.println("通过虚拟文件系统读取的内容: " + virtualContent);
                    System.out.println("内容同步: " + virtualContent.equals(modifiedContent));
                    fs.closeFile(fd);
                }
            } catch (Exception e) {
                System.out.println("读取/写入真实文件时出错: " + e.getMessage());
            }
        }
        
        // 删除虚拟文件
        boolean deleteResult = fs.deleteFile(virtualFilePath);
        System.out.println("删除虚拟文件: " + (deleteResult ? "成功" : "失败"));
        
        // 检查真实文件是否也被删除
        boolean realFileDeleted = !Files.exists(realFilePath);
        System.out.println("真实文件已删除: " + realFileDeleted);
        
        System.out.println("----------------------真实文件系统映射测试完成!----------------------");
    }
} 