package main.java;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueSize;
    private final long keepAliveTime;
    private final int minSpareThreads;
    private final AtomicInteger idleWorkers = new AtomicInteger(0);
    private final AtomicInteger rejectedCount = new AtomicInteger(0);

    private final List<CustomBlockingQueue> queueList = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new CopyOnWriteArrayList<>();

    private final AtomicInteger roundRobinInd = new AtomicInteger(0);

    private final ThreadFactory threadFactory;

    private volatile boolean isShutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize,
                            int queueSize, long keepAliveTime,
                            TimeUnit unit, int minSpareThreads) {

        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTime = unit.toMillis(keepAliveTime);
        this.minSpareThreads = minSpareThreads;

        this.threadFactory = new CustomThreadFactory("MyPool");

        initCoreWorkers();
    }

    @Override
    public void execute(Runnable r) {
        if (isShutdown) {
            onReject(r);
            return;
        }

        ensureMinSpareThreads();

        CustomBlockingQueue queue = selectQueue();

        if (!queue.offer(r)) handleRejected(r);
        else System.out.println("[Pool] Task accepted into queue #" + queue.getId() + ": " + r);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        execute(task);
        return task;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        System.out.println("[Pool] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        System.out.println("[Pool] ShutdownNow initiated. Interrupting all workers.");
        for (Thread t : workers) {
            t.interrupt();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long timeLeft = System.currentTimeMillis() + unit.toMillis(timeout);
        synchronized (this) {
            while (!workers.isEmpty()) {
                long remaining = timeLeft - System.currentTimeMillis();
                if (remaining <= 0) return false;
                wait(remaining);
            }
        }
        return true;
    }

    public synchronized void onWorkerEnd(Thread thread) {
        workers.remove(thread);
        notifyAll();
    }

    public synchronized void removeQueue(CustomBlockingQueue queue) {
        queueList.remove(queue);
    }

    void resubmit(Runnable task) {
        if (queueList.isEmpty()) {
            onReject(task);
            return;
        }

        CustomBlockingQueue q = selectQueue();

        if (!q.offer(task)) onReject(task);
        else System.out.println("[Pool] Task transferred into queue #" + q.getId() + ": " + task);
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public boolean shouldTerminateWorker() {
        return workers.size() > corePoolSize;
    }

    public void incIdle() {
        idleWorkers.incrementAndGet();
    }

    public void decIdle() {
        idleWorkers.decrementAndGet();
    }


    private void initCoreWorkers() {
        for (int i = 0; i < corePoolSize; i++) {
            CustomBlockingQueue queue = new CustomBlockingQueue(i, queueSize);
            queueList.add(queue);

            Worker worker = new Worker(queue, keepAliveTime, this);
            Thread thread = threadFactory.newThread(worker);

            workers.add(thread);
            thread.start();
        }
    }

    private CustomBlockingQueue selectQueue() {
        int index = (roundRobinInd.getAndIncrement() & Integer.MAX_VALUE) % queueList.size();
        return queueList.get(index);
    }

    private synchronized void addWorker(Runnable firstTask) {
        if (workers.size() >= maxPoolSize) {
            if (firstTask != null) onReject(firstTask);
            return;
        }

        int id = queueList.size();
        CustomBlockingQueue queue = new CustomBlockingQueue(id, queueSize);

        if (firstTask != null) {
            queue.offer(firstTask);
        }

        queueList.add(queue);

        Worker worker = new Worker(queue, keepAliveTime, this);
        Thread thread = threadFactory.newThread(worker);

        workers.add(thread);
        thread.start();
    }

    private void handleRejected(Runnable command) {
        if (workers.size() < maxPoolSize) {
            addWorker(command);
        } else {
            onReject(command);
        }
    }

    private void onReject(Runnable command) {
        System.out.println("[Pool] (Rejected) Task " + command + " was rejected due to overload!");
        System.out.println(
                "(total rejected: " + rejectedCount.incrementAndGet() +
                ", workers: " + workers.size() + "/" + maxPoolSize +
                ", reason: all queues full and maxPoolSize reached)");
    }

    private void ensureMinSpareThreads() {
        while (idleWorkers.get() < minSpareThreads &&
                workers.size() < maxPoolSize) {

            System.out.println("[Pool] Creating spare worker. Idle: "
                    + idleWorkers.get());

            addWorker(null);
        }
    }
}
