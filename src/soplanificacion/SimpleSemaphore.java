package soplanificacion;
import java.util.concurrent.TimeUnit;

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
    
    public synchronized boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        // 1. Convierte el tiempo de espera a milisegundos
        long timeoutMs = unit.toMillis(timeout);
        long endTime = System.currentTimeMillis() + timeoutMs; // Hora de finalización

        // 2. Bucle de espera (maneja "despertares espurios")
        while (permits == 0) {
            long remainingTime = endTime - System.currentTimeMillis();
            
            // 3. Comprueba si el tiempo se agotó
            if (remainingTime <= 0) {
                return false; // Timeout
            }
            
            // 4. Espera por el tiempo restante
            wait(remainingTime);
        }
        
        // 5. Permiso adquirido
        permits--;
        return true;
    }
    
}