package jp.oist.abcvlib.util;

import java.util.concurrent.ThreadFactory;

public final class ProcessPriorityThreadFactory implements ThreadFactory {

    private final int threadPriority;
    private final String threadName;
    private int threadCount = 0;

    public ProcessPriorityThreadFactory(int threadPriority, String threadName) {
        this.threadPriority = threadPriority;
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setPriority(threadPriority);
        thread.setName(threadName + "_" + threadCount);
        threadCount++;
        return thread;
    }

}
