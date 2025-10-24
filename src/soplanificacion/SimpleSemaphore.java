package soplanificacion;

/**
 * Semáforo simple sin java.util.concurrent.
 * Soporta acquire/release con N permisos.
 */
public final class SimpleSemaphore {
    private int permits;

    public SimpleSemaphore() { this(1); }
    public SimpleSemaphore(int permits) {
        if (permits < 0) permits = 0;
        this.permits = permits;
    }

    public synchronized void acquire() throws InterruptedException {
        while (permits == 0) wait();
        permits--;
    }
    public synchronized void release() {
        permits++;
        notifyAll();
    }
}