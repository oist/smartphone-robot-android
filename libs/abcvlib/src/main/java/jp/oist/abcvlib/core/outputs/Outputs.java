package jp.oist.abcvlib.core.outputs;

import java.util.concurrent.TimeUnit;

import ioio.lib.api.exception.ConnectionLostException;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;
import jp.oist.abcvlib.util.SerialCommManager;

public class Outputs {

    public Motion motion;
    private final MasterController masterController;
    private final ScheduledExecutorServiceWithException threadPoolExecutor;
    private final SerialCommManager serialCommManager;

    public Outputs(Switches switches, SerialCommManager serialCommManager){
        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the MasterController
        this.serialCommManager = serialCommManager;
        ProcessPriorityThreadFactory processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "Outputs");
        threadPoolExecutor = new ScheduledExecutorServiceWithException(threadCount, processPriorityThreadFactory);

        //BalancePIDController Controller
        motion = new Motion(switches);

        masterController = new MasterController(switches, serialCommManager);
    }

    public void startMasterController(){
        threadPoolExecutor.scheduleWithFixedDelay(masterController, 0, 1, TimeUnit.MILLISECONDS);
    }

    /**
     * @param left speed from -1 to 1 (full speed backward vs full speed forward)
     * @param right speed from -1 to 1 (full speed backward vs full speed forward)
     */
    public void setWheelOutput(float left, float right, boolean leftBrake, boolean rightBrake) {
        serialCommManager.setMotorLevels(left, right, leftBrake, rightBrake);
    }

    public synchronized MasterController getMasterController() {
        return masterController;
    }

    public void turnOffWheels() throws ConnectionLostException {
        setWheelOutput(0, 0, false, false);
    }
}
