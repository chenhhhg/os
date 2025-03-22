package os.process;

import os.system.SystemTimer;

public interface Scheduler {
    // 处理时钟中断（返回是否需要调度）
    boolean onTick(SystemTimer timer);

    // 添加新进程
    void addProcess(PCB process);

    // 获取当前运行进程
    PCB getRunningProcess();

    // 执行上下文切换
    void performContextSwitch();
}
