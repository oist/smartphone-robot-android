package jp.oist.abcvlib.servicetest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibService;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.outputs.AbcvlibController;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class MyAbcvlibService extends AbcvlibService implements IOReadyListener {

    ScheduledExecutorServiceWithException executor = new ScheduledExecutorServiceWithException(
            1, new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY,
            "BackAndForthController"));

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setIoReadyListener(this);
        int result = super.onStartCommand(intent, flags, startId);
        return result;
    }

    @Override
    public void onIOReady() {
        float speed = 0.5f;
        BackAndForthController backAndForthController = new BackAndForthController(speed);

        // Add the custom controller to the grand controller (controller that assembles other controllers)
        getOutputs().getMasterController().addController(backAndForthController);
        executor.scheduleAtFixedRate(backAndForthController, 0, 1000, TimeUnit.MILLISECONDS);
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
