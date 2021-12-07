package jp.oist.abcvlib.core.outputs;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public abstract class AbcvlibController implements Runnable{

    private String name;
    private int threadCount = 1;
    private int threadPriority = Thread.NORM_PRIORITY;
    private int initDelay;
    private int timeStep;
    private TimeUnit timeUnit;
    private ScheduledExecutorServiceWithException executor;
    private final String TAG = getClass().getName();
    private boolean isRunning = false;

    public AbcvlibController(){}

    public AbcvlibController setName(String name) {
        this.name = name;
        return this;
    }

    public AbcvlibController setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public AbcvlibController setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
        return this;
    }

    public AbcvlibController setInitDelay(int initDelay) {
        this.initDelay = initDelay;
        return this;
    }

    public AbcvlibController setTimestep(int timeStep) {
        this.timeStep = timeStep;
        return this;
    }

    public AbcvlibController setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public void startController(){
        executor = new ScheduledExecutorServiceWithException(
                threadCount, new ProcessPriorityThreadFactory(threadPriority,
                name));
        executor.scheduleAtFixedRate(this, initDelay, timeStep, timeUnit);
        isRunning = true;
    }

    public void stopController(){
        setOutput(0,0);
        executor.shutdown();
        isRunning = false;
    }

    public Output output = new Output();

    synchronized Output getOutput(){
        return output;
    }

    protected synchronized void setOutput(float left, float right){
        output.left = left;
        output.right = right;
    }

    @Override
    public void run() {
        ErrorHandler.eLog(TAG, "You must override the run method within your custom AbcvlibController.", new Exception(),false);
    }

    public static class Output{
        public float left;
        public float right;
    }

    public synchronized boolean isRunning(){
        return isRunning;
    }
}
