package os.process;

public enum ProcessState {
    //新建
    NEW,
    //就绪
    READY,
    //运行中
    RUNNING,
    //阻塞
    BLOCKED,
    //终止
    TERMINATED
}
