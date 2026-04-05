package main.java;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CustomBlockingQueue {
    private final int id;
    private final BlockingQueue<Runnable> queue;

    public CustomBlockingQueue(int id, int capacity) {
        this.id = id;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(Runnable task) {
        return queue.offer(task);
    }

    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int getId() {
        return id;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }
}
