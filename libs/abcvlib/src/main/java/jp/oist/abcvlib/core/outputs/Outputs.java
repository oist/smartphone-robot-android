package jp.oist.abcvlib.core.outputs;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class Outputs {

    public Motion motion;
    private final MasterController masterController;
    private final ScheduledExecutorServiceWithException threadPoolExecutor;
    private final AbcvlibLooper abcvlibLooper;

    public Outputs(Switches switches, AbcvlibLooper abcvlibLooper){
        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the MasterController
        this.abcvlibLooper = abcvlibLooper;
        ProcessPriorityThreadFactory processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "Outputs");
        threadPoolExecutor = new ScheduledExecutorServiceWithException(threadCount, processPriorityThreadFactory);

        //BalancePIDController Controller
        motion = new Motion(switches);

        masterController = new MasterController(switches, abcvlibLooper);
    }

    public void startMasterController(){
        threadPoolExecutor.scheduleWithFixedDelay(masterController, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * @param left speed from -1 to 1 (full speed backward vs full speed forward)
     * @param right speed from -1 to 1 (full speed backward vs full speed forward)
     */
    public void setWheelOutput(float left, float right) {
        abcvlibLooper.setDutyCycle(left, right);
    }

    public synchronized MasterController getMasterController() {
        return masterController;
    }
}
