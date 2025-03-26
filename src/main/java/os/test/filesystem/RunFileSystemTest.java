package os.test.filesystem;

/**
 * 文件系统测试启动类
 */
public class RunFileSystemTest {
    public static void main(String[] args) {
        System.out.println("========== 启动文件系统测试 ==========");
        
        // 运行文件系统测试
        FileSystemTest.main(args);
        
        System.out.println("========== 文件系统测试结束 ==========");
    }
} 