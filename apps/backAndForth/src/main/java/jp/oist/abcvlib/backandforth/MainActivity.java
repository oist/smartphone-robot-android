package jp.oist.abcvlib.backandforth;

import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.tests.BackAndForthController;

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

    @Override
    public void onIOReady() {
        float speed = 0.5f;
        // Customizing ALL build params. You can remove any or all. This object not used, but here for reference.
        BackAndForthController backAndForthController = (BackAndForthController) new BackAndForthController(speed).setInitDelay(0)
                .setName("BackAndForthController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(1000)
                .setTimeUnit(TimeUnit.MILLISECONDS);

        // Start your custom controller
        backAndForthController.startController();
        // Adds your custom controller to the compounding master controller.
        getOutputs().getMasterController().addController(backAndForthController);
        // Start the master controller after adding and starting any customer controllers.
        getOutputs().startMasterController();
    }
}
