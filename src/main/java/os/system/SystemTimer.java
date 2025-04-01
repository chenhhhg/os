package os.system;

public class SystemTimer {
    private long ticks = 0;

    public void tick() {
        ticks++;
    }
    public long getTicks() { return ticks; }
}
