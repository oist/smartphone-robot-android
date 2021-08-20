package jp.oist.abcvlib.servicetest;

import android.content.Intent;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibService;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

public class MyAbcvlibService extends AbcvlibService implements IOReadyListener {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setIoReadyListener(this);
        return super.onStartCommand(intent, flags, startId);
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

        // Start the controller only after IO is ready.
        backAndForthController.start();
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
