package jp.oist.abcvlib.util;

import android.util.Log;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceWithException {

    private final ScheduledExecutorService executor;
    private final String TAG = getClass().getName();

    public ScheduledExecutorServiceWithException(int corePoolSize, ThreadFactory threadFactory) {
        executor = Executors.newScheduledThreadPool(corePoolSize, threadFactory);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long delay, TimeUnit unit){

        ScheduledFuture<?> scheduledFuture = executor
                .scheduleWithFixedDelay(command, initialDelay, delay, unit);

        catchErrors(scheduledFuture);

        return scheduledFuture;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit){

        ScheduledFuture<?> scheduledFuture = executor
                .scheduleWithFixedDelay(command, initialDelay, delay, unit);

        catchErrors(scheduledFuture);

        return scheduledFuture;
    }

    public void execute(Runnable command){
        ScheduledFuture<?> scheduledFuture = executor
                .schedule(command, 0,TimeUnit.MILLISECONDS);

        catchErrors(scheduledFuture);
    }

    private void catchErrors(ScheduledFuture<?> scheduledFuture){
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("before get()");
                    scheduledFuture.get(); // will return only if canceled
                    System.out.println("after get()");
                } catch (ExecutionException e) {
                    executor.shutdown();
                    throw new RuntimeException(e);
                } catch (InterruptedException | CancellationException e){
                    Log.d(TAG, "Executor Interrupted or Cancelled", e);
                }
            }
        });
    }

    public void shutdownNow(){
        executor.shutdownNow();
    }
}
