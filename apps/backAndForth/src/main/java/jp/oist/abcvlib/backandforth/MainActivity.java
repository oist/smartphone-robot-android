package jp.oist.abcvlib.backandforth;

import static java.lang.Thread.sleep;

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.util.SerialReadyListener;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Also includes a simple controller making the robot move back and forth at a set interval and speed
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setIoReadyListener(this);
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
    }

    class BackAndForth implements Runnable {
        private boolean switchMethod = false;

        @Override
        public void run() {
            if (switchMethod) {
                Log.d("BackAndForth", "Setting motor levels to 0.5");
//                serialCommManager.setMotorLevels(0.5f, 0.5f);
                serialCommManager.setMotorLevels(1f, 1f);
            } else {
                Log.d("BackAndForth", "Setting motor levels to -0.5");
//                serialCommManager.setMotorLevels(-0.5f, -0.5f);
                serialCommManager.setMotorLevels(-1f, -1f);
            }
            switchMethod = !switchMethod;
        }
    }

    @Override
    public void onIOReady() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        Runnable backAndForth = new BackAndForth();

//        serialCommManager.setMotorLevels(-0.5f, -0.5f);
//        serialCommManager.setMotorLevels(0.5f, 0.5f);
//        serialCommManager.setMotorLevels(0f, 0f);

        // Schedule the task to run every 2 seconds + whatever time it takes to run the task
        executorService.scheduleWithFixedDelay(backAndForth, 0, 100, TimeUnit.MILLISECONDS);

//        // Keep the program running for a while to observe the scheduled task
//        try {
//            Thread.sleep(10000); // Run for 10 seconds
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Shutdown the executor service when done
//        executorService.shutdown();

//        // TODO refactor master controllers and AbcvlibController to use rp2040 instead of IOIO
//        float speed = 0.5f;
//        // Customizing ALL build params. You can remove any or all. This object not used, but here for reference.
//        BackAndForthController backAndForthController = (BackAndForthController) new BackAndForthController(speed).setInitDelay(0)
//                .setName("BackAndForthController").setThreadCount(1)
//                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(1000)
//                .setTimeUnit(TimeUnit.MILLISECONDS);
//
//        // Start your custom controller
//        backAndForthController.startController();
//        // Adds your custom controller to the compounding master controller.
//        getOutputs().getMasterController().addController(backAndForthController);
//        // Start the master controller after adding and starting any customer controllers.
//        getOutputs().startMasterController();
    }
}
