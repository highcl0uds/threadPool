package main.java;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(1);
    private final String poolName;

    public CustomThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = poolName + "-worker-" + counter.getAndIncrement();
        System.out.println("[ThreadFactory] Creating new thread: " + name);

        return new Thread(r, name);
    }
}
