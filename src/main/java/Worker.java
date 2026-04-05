package main.java;

import java.util.concurrent.TimeUnit;

public class Worker implements Runnable {
    private final CustomBlockingQueue queue;
    private final long keepAliveTime;
    private final CustomThreadPool pool;

    public Worker(CustomBlockingQueue queue,
                  long keepAliveTime,
                  CustomThreadPool pool) {
        this.queue = queue;
        this.keepAliveTime = keepAliveTime;
        this.pool = pool;
    }

    @Override
    public void run() {
        String currentThreadName = Thread.currentThread().getName();
        boolean idleTimeout = false;

        try {
            while (true) {
                boolean shuttingDown = pool.isShutdown();

                pool.incIdle();
                Runnable task = queue.poll(shuttingDown ? 0 : keepAliveTime, TimeUnit.MILLISECONDS);
                pool.decIdle();

                if (task == null) {
                    if (pool.isShutdown()) break; // перечитываем: мог стать true пока ждали poll
                    if (pool.shouldTerminateWorker()) {
                        System.out.println("[Worker] " + currentThreadName + " idle timeout, stopping.");
                        idleTimeout = true;
                        break;
                    }
                    continue;
                }

                System.out.println("[Worker] " + currentThreadName + " executes " + task);
                task.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (idleTimeout) {
                pool.removeQueue(queue);
                transferLeftTasks(currentThreadName);
            }
            System.out.println("[Worker] " + currentThreadName + " terminated.");
            pool.onWorkerEnd(Thread.currentThread());
        }
    }

    private void transferLeftTasks(String threadName) {
        try {
            Runnable task;
            while ((task = queue.poll(0, TimeUnit.MILLISECONDS)) != null) {
                System.out.println("[Worker] " + threadName + " transferring left task: " + task);
                pool.resubmit(task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
