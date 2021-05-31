package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class Outputs implements OutputsInterface {

    private Thread centerBlobControllerThread;
    protected Thread pidControllerThread;
    public Motion motion;
    private Thread socketClientThread;
    private volatile BalancePIDController balancePIDController;
    private GrandController grandController;
    private Thread grandControllerThread;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();
    private ProcessPriorityThreadFactory processPriorityThreadFactory;
    private ScheduledExecutorServiceWithException threadPoolExecutor;


    public Outputs(Switches switches, AbcvlibLooper abcvlibLooper, Inputs inputs){

        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the GrandController
//        threadCount += (switches.pythonControlledPIDBalancer) ? 1 : 0;
//        threadCount += (switches.balanceApp) ? 1 : 0;
//        threadCount += (switches.centerBlobApp) ? 1 : 0;
        processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "Outputs");
        threadPoolExecutor = new ScheduledExecutorServiceWithException(threadCount, processPriorityThreadFactory);

        //BalancePIDController Controller
        motion = new Motion(switches);
        Log.v("abcvlib", "motion object created");


        if (switches.balanceApp){
            balancePIDController = new BalancePIDController(switches, inputs);
            threadPoolExecutor.scheduleWithFixedDelay(balancePIDController, 0, 2000, TimeUnit.MILLISECONDS);
            controllers.add(balancePIDController);
            Log.i("abcvlib", "BalanceApp Started");
        }

        if (!controllers.isEmpty()){
            grandController = new GrandController(switches, abcvlibLooper);
            for (AbcvlibController controller: controllers){
                grandController.addController(controller);
            }
            threadPoolExecutor.scheduleWithFixedDelay(grandController, 0, 4000, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void setControls(JSONObject controls) {

    }

    @Override
    public void setAudioFile() {

    }

    @Override
    public void setWheelOutput(int left, int right) {

    }

    @Override
    public void setPID() {

    }

    public synchronized BalancePIDController getBalancePIDController() {
        return balancePIDController;
    }
}
