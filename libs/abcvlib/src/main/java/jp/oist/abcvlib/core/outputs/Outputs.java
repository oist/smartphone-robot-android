package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class Outputs {

    private Thread centerBlobControllerThread;
    protected Thread pidControllerThread;
    public Motion motion;
    private Thread socketClientThread;
    private volatile BalancePIDController balancePIDController;
    private MasterController masterController;
    private Thread grandControllerThread;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();
    private ProcessPriorityThreadFactory processPriorityThreadFactory;
    private ScheduledExecutorServiceWithException threadPoolExecutor;
    private AbcvlibLooper abcvlibLooper;


    public Outputs(Switches switches, AbcvlibLooper abcvlibLooper, Inputs inputs){

        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the MasterController
//        threadCount += (switches.pythonControlledPIDBalancer) ? 1 : 0;
//        threadCount += (switches.balanceApp) ? 1 : 0;
//        threadCount += (switches.centerBlobApp) ? 1 : 0;
        this.abcvlibLooper = abcvlibLooper;
        processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "Outputs");
        threadPoolExecutor = new ScheduledExecutorServiceWithException(threadCount, processPriorityThreadFactory);

        //BalancePIDController Controller
        motion = new Motion(switches);
        Log.v("abcvlib", "motion object created");


        if (switches.balanceApp){
            balancePIDController = new BalancePIDController(switches, inputs);
            threadPoolExecutor.scheduleWithFixedDelay(balancePIDController, 0, 1, TimeUnit.MILLISECONDS);
            controllers.add(balancePIDController);
            Log.i("abcvlib", "BalanceApp Started");
        }

        if (!controllers.isEmpty()){
            masterController = new MasterController(switches, abcvlibLooper);
            for (AbcvlibController controller: controllers){
                masterController.addController(controller);
            }
            threadPoolExecutor.scheduleWithFixedDelay(masterController, 0, 1, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * @param left speed from -1 to 1 (full speed backward vs full speed forward)
     * @param right speed from -1 to 1 (full speed backward vs full speed forward)
     */
    public void setWheelOutput(float left, float right) {
        abcvlibLooper.setDutyCycle(left, right);
    }

    public synchronized BalancePIDController getBalancePIDController() {
        return balancePIDController;
    }

    public synchronized MasterController getMasterController() {
        return masterController;
    }
}
