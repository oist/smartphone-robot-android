package jp.oist.abcvlib.core.outputs;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibAbstractObject;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class AbcvlibController implements Runnable{

    private String name;
    private int threadCount = 1;
    private int threadPriority = Thread.NORM_PRIORITY;
    private int initDelay;
    private int timeStep;
    private TimeUnit timeUnit;
    private ScheduledExecutorServiceWithException executor;
    private final String TAG = getClass().getName();

    public AbcvlibController(){}

    public AbcvlibController(AbcvlibController abcvlibController,
                                    AbcvlibAbstractObject abcvlibAbstractObject, String name,
                                    int threadCount, int threadPriority, int initDelay,
                                    int timeStep, TimeUnit timeUnit){
        // Add the custom controller to the grand controller (controller that assembles other controllers)
        abcvlibAbstractObject.getOutputs().getMasterController().addController(abcvlibController);
        this.name = name;
        this.threadCount = threadCount;
        this.threadPriority  = threadPriority;
        this.initDelay = initDelay;
        this.timeStep = timeStep;
        this.timeUnit = timeUnit;
    }

    public void start(){
        executor = new ScheduledExecutorServiceWithException(
                threadCount, new ProcessPriorityThreadFactory(threadPriority,
                name));
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

    public static class AbcvlibControllerBuilder {
        private final AbcvlibController abcvlibController;
        private final AbcvlibAbstractObject abcvlibAbstractObject;
        private String name;
        private int threadCount = 1;
        private int threadPriority = Thread.NORM_PRIORITY;
        private int initDelay = 0;
        private int timeStep = 1;
        private TimeUnit timeUnit = TimeUnit.SECONDS;

        /**
         * @param abcvlibAbstractObject Typically the keyword "this" if calling from a child class of AbcvlibActivity or AbcvlibService
         * @param abcvlibController The custom AbcvlibController you have defined.
         */
        public AbcvlibControllerBuilder(AbcvlibAbstractObject abcvlibAbstractObject,
                                        AbcvlibController abcvlibController) {
            this.abcvlibController = abcvlibController;
            this.abcvlibAbstractObject = abcvlibAbstractObject;
        }

        public AbcvlibController build() {
            return new AbcvlibController(abcvlibController, abcvlibAbstractObject, name,
            threadCount, threadPriority, initDelay, timeStep, timeUnit);
        }

        public AbcvlibControllerBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public AbcvlibControllerBuilder setThreadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public AbcvlibControllerBuilder setThreadPriority(int threadPriority) {
            this.threadPriority = threadPriority;
            return this;
        }

        public AbcvlibControllerBuilder setInitDelay(int initDelay) {
            this.initDelay = initDelay;
            return this;
        }

        public AbcvlibControllerBuilder setTimestep(int timeStep) {
            this.timeStep = timeStep;
            return this;
        }

        public AbcvlibControllerBuilder setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }
    }
}
