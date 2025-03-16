package os.process;

public interface ProcessController {
    PCB createProcess();
    boolean terminateProcess();
}
