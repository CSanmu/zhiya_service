package util;

import java.util.concurrent.*;

/**
 * @author chenzengsen
 * @date 2024/3/25 9:22 上午
 */
public class ThreadPools {
    private final ExecutorService small;
    private final ExecutorService big;

    public ThreadPools() {
        small = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());
        big = new ThreadPoolExecutor(4, 4,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1024),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static ThreadPools getInstance() {
        return ThreadPoolManagerHolder.instance;
    }

    private static class ThreadPoolManagerHolder {
        static ThreadPools instance = new ThreadPools();
    }

    public void submitSmallTask(Runnable task) {
        try {
            small.submit(task);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void submitBigTask(Runnable task) {
        try {
            big.submit(task);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }
}
