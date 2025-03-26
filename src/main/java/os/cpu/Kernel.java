package os.cpu;

// 操作系统内核函数
public class Kernel {
    // 处理内存访问系统调用
    public static void handleMemoryAccess(int... values) {
        System.out.println("\n[Kernel] Memory Access Report:");
        System.out.println("Address 1024: " + values[0]);
        System.out.println("Address 2025: " + values[1]);
        System.out.println("Address 3026: " + values[2]);
        System.out.println("Address 4027: " + values[3]);
    }
}
