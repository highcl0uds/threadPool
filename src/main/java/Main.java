package main.java;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("---CustomThreadPool DEMO---\n\n\n");

        runNormalLoadScenario();
        System.out.println("--------------------------\n");

        runFullLoadScenario();
        System.out.println("--------------------------\n");

        runOverloadScenario();
        System.out.println("--------------------------\n");

        runForcedShutdownScenario();
        System.out.println("--------------------------\n");

        runGracefulShutdownScenario();
        System.out.println("--------------------------\n");


        System.out.println("\n\n\n---DEMO END---");
    }

    private static void runNormalLoadScenario() {
        System.out.println("SCENARIO 1. NORMAL LOAD. 8 tasks, pool capacity 16 \n");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 3, 5, TimeUnit.SECONDS, 2
        );

        for (int i = 1; i <= 8; i++) {
            pool.execute(task(i, 1500));
        }

        runTermination(pool, 60);

        System.out.println("SCENARIO 1. COMPLETED.\n");
    }

    private static void runFullLoadScenario() {
        System.out.println();
        System.out.println("SCENARIO 2. FULL LOAD. 14 tasks, pool capacity 16\n");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 3, 5, TimeUnit.SECONDS, 2
        );

        for (int i = 1; i <= 14; i++) {
            pool.execute(task(i, 2000));
        }

        runTermination(pool, 60);

        System.out.println("SCENARIO 2. COMPLETED.\n");
    }

    private static void runOverloadScenario() {
        System.out.println();
        System.out.println("SCENARIO 3. OVERLOAD (with REJECTIONS). 25 tasks, pool capacity 16\n");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 3, 5, TimeUnit.SECONDS, 2
        );

        for (int i = 1; i <= 25; i++) {
            pool.execute(task(i, 2000));
        }

        runTermination(pool, 60);

        System.out.println("SCENARIO 3. COMPLETED.\n");
    }

    private static void runForcedShutdownScenario() {
        System.out.println();
        System.out.println("SCENARIO 4. FORCED SHUTDOWN. All runed tasks interrupted\n");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 3, 5, TimeUnit.SECONDS, 2
        );

        for (int i = 1; i <= 8; i++) {
            pool.execute(task(i, 10000));
        }

        runTermination(pool, 1);

        System.out.println("SCENARIO 4. COMPLETED.\n");
    }

    private static void runGracefulShutdownScenario() {
        System.out.println();
        System.out.println("SCENARIO 5. GRACEFUL SHUTDOWN. All queued tasks completed\n");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 3, 5, TimeUnit.SECONDS, 2
        );

        for (int i = 1; i <= 8; i++) {
            pool.execute(task(i, 1000));
        }

        runTermination(pool, 30);
        pool.execute(task(1000, 500));

        System.out.println("SCENARIO 5. COMPLETED.\n");
    }

    private static Runnable task(int id, int sleepMs) {
        return new Runnable() {
            @Override
            public void run() {
                System.out.println("*Task-" + id + " started");
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    System.out.println("*Task-" + id + " interrupted");
                    Thread.currentThread().interrupt();
                    return;
                }
                System.out.println("*Task-" + id + " finished");
            }

            @Override
            public String toString() {
                return "task-" + id;
            }
        };
    }

    private static void runTermination(CustomThreadPool pool, long timeout) {
        try {
            Thread.sleep(1000);
            pool.shutdown();

            boolean terminated = pool.awaitTermination(timeout, TimeUnit.SECONDS);
            System.out.println("Pool terminated gracefully: " + terminated);

            if (!terminated) {
                pool.shutdownNow();
                pool.awaitTermination(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            System.out.println("InterruptedException: " + e);
        }
    }
}
