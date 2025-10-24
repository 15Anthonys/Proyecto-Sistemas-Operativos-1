package soplanificacion;

/**
 * Contador seguro sin Atomic*.
 */
public final class SafeCounter {
    private long value;
    public SafeCounter(){ this(0); }
    public SafeCounter(long initial){ this.value = initial; }

    public synchronized long get(){ return value; }
    public synchronized long set(long v){ this.value = v; return value; }
    public synchronized long incrementAndGet(){ value++; return value; }
    public synchronized long decrementAndGet(){ value--; return value; }
}