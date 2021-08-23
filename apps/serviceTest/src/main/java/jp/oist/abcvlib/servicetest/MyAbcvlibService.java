package jp.oist.abcvlib.servicetest;

import android.content.Intent;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibService;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.outputs.BackAndForthController;

public class MyAbcvlibService extends AbcvlibService implements IOReadyListener {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setIoReadyListener(this);
        return super.onStartCommand(intent, flags, startId);
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
