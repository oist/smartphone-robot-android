package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
    private AbcvlibLooper abcvlibLooper;


    public Outputs(Switches switches, AbcvlibLooper abcvlibLooper, Inputs inputs){

        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the GrandController
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
            grandController = new GrandController(switches, abcvlibLooper);
            for (AbcvlibController controller: controllers){
                grandController.addController(controller);
            }
            threadPoolExecutor.scheduleWithFixedDelay(grandController, 0, 1, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void setControls(JSONObject controls) {

    }

    @Override
    public void setAudioFile() {

    }

    /**
     * @param left speed from -1 to 1 (full speed backward vs full speed forward)
     * @param right speed from -1 to 1 (full speed backward vs full speed forward)
     */
    @Override
    public void setWheelOutput(float left, float right) {
        abcvlibLooper.setDutyCycle(left, right);
    }

    @Override
    public void setPID() {

    }

    public synchronized BalancePIDController getBalancePIDController() {
        return balancePIDController;
    }
}
