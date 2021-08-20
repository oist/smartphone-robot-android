package jp.oist.abcvlib.backandforth;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.outputs.AbcvlibController;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    private ScheduledExecutorServiceWithException executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setIoReadyListener(this);
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
        executor = new ScheduledExecutorServiceWithException(1,
                new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "BackAndForthController"));
    }

    @Override
    public void onIOReady() {
        float speed = 0.5f;
        // Using Builder with default build params used
        AbcvlibController backAndForthController = new AbcvlibController.AbcvlibControllerBuilder(
                this, new BackAndForthController(speed)).build();
        // Customizing ALL build params. You can remove any or all. This object not used, but here for reference.
        AbcvlibController backAndForthController2 = new AbcvlibController.AbcvlibControllerBuilder(
                this, new BackAndForthController(speed))
                .setName("BackAndForthController").setTimestep(1).setTimeUnit(TimeUnit.SECONDS)
                .setInitDelay(0).setThreadCount(1).setThreadPriority(Thread.MAX_PRIORITY).build();

        // Start your custom controller
        backAndForthController.startController();
        // Start the master controller after adding any customer controllers.
        getOutputs().startMasterController();
    }

    public static class BackAndForthController extends AbcvlibController {
        float speed;
        float currentSpeed;
        public BackAndForthController(float speed){
            this.speed = speed;
            this.currentSpeed = speed;
        }
        @Override
        public void run() {
            if (currentSpeed == speed){
                currentSpeed = -speed;
            }else {
                currentSpeed = speed;
            }
            setOutput(currentSpeed, currentSpeed);
        }
    }
}
