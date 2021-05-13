package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class Outputs implements OutputsInterface {

    private Thread centerBlobControllerThread;
    private CenterBlobController centerBlobController;
    protected Thread pidControllerThread;
    public Motion motion;
    private Thread socketClientThread;
    public BalancePIDController balancePIDController;
    private GrandController grandController;
    private Thread grandControllerThread;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();
    private ProcessPriorityThreadFactory processPriorityThreadFactory;
    private ScheduledThreadPoolExecutor threadPoolExecutor;

    public Outputs(AbcvlibActivity abcvlibActivity,
                   AbcvlibController customController){

        // Determine number of necessary threads.
        int threadCount = 1; // At least one for the GrandController
        threadCount += (abcvlibActivity.switches.pythonControlledPIDBalancer) ? 1 : 0;
        threadCount += (abcvlibActivity.switches.balanceApp) ? 1 : 0;
        threadCount += (abcvlibActivity.switches.centerBlobApp) ? 1 : 0;
        processPriorityThreadFactory = new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "Outputs");
        threadPoolExecutor = new ScheduledThreadPoolExecutor(threadCount, processPriorityThreadFactory);

        // Add custom controller if specified
        if (customController != null){
            threadPoolExecutor.scheduleAtFixedRate(customController, 0, 1, TimeUnit.MILLISECONDS);
            controllers.add(customController);
        }

        //BalancePIDController Controller
        motion = new Motion(abcvlibActivity);
        Log.v("abcvlib", "motion object created");


        if (abcvlibActivity.switches.balanceApp){
            balancePIDController = new BalancePIDController(abcvlibActivity);
            threadPoolExecutor.scheduleAtFixedRate(balancePIDController, 0, 1, TimeUnit.MILLISECONDS);
            controllers.add(balancePIDController);
            Log.i("abcvlib", "BalanceApp Started");
        }

        // Todo need some method to handle combining balancePIDController output with another controller
        //  (centerBlobApp, setPath, etc.). Maybe some grandController on another thread that reads in the
        //  output from balancePIDController along with the output from e.g. centerBlobApp() and path().
        if (abcvlibActivity.switches.centerBlobApp){
            centerBlobController = new CenterBlobController(abcvlibActivity);
            threadPoolExecutor.scheduleAtFixedRate(centerBlobController, 0, 1, TimeUnit.MILLISECONDS);
            controllers.add(centerBlobController);
        }

        if (!controllers.isEmpty()){
            grandController = new GrandController(abcvlibActivity, controllers);
            threadPoolExecutor.scheduleAtFixedRate(grandController, 0, 1, TimeUnit.MILLISECONDS);
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

}
