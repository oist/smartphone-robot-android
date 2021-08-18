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
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

public class MyAbcvlibService extends AbcvlibService implements IOReadyListener {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setIoReadyListener(this);
        int result = super.onStartCommand(intent, flags, startId);
        return result;
    }

    @Override
    public void onIOReady() {
        float speed = 1.0f;
        float[][] speedProfile = {{speed, 0, -speed, 0}, {speed, 0, -speed, 0}, {2000, 1000, 2000, 1000}};
        BackAndForth backAndForth = new BackAndForth(speedProfile);
        backAndForth.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BackAndForth {
        /**
         * 3xn matrix with column c, row 1 representing speed of left wheel
         * row 2 representing speed of right wheel, and row 3 the time window for
         * for speed c. Speed specified as -1 to 1 representing maximum speed backward and forward.
         * Time window specified in milliseconds
         */
        float[][] speedProfile;
        ScheduledExecutorServiceWithException executor;
        SpeedSetter speedSetter;

        @RequiresApi(api = Build.VERSION_CODES.N)
        public BackAndForth(float[][] speedProfile){
            this.speedProfile = speedProfile;

            executor = new ScheduledExecutorServiceWithException(1,
                    new ProcessPriorityThreadFactory(Thread.NORM_PRIORITY, "BackAndForth"));
        }

        public void start(){
            long totalTime = 0;
            for (float timeLen : speedProfile[2]){
                totalTime+=timeLen;
            }
            executor.scheduleAtFixedRate(() -> {
                int endOfCurrentTimeSlot = 0;
                for (int i = 0 ; i < speedProfile[0].length; i++){
                    speedSetter = new SpeedSetter(speedProfile[0][i], speedProfile[1][i]);
                    executor.schedule(speedSetter, endOfCurrentTimeSlot, TimeUnit.MILLISECONDS);
                    endOfCurrentTimeSlot+=speedProfile[2][i];
                }
            }, 0, totalTime, TimeUnit.MILLISECONDS);
        }
    }

    class SpeedSetter implements Runnable{

        private final float left;
        private final float right;

        public SpeedSetter(float left, float right){
            this.left = left;
            this.right = right;
        }

        @Override
        public void run() {
            if (getOutputs() == null){
                Log.d("backandforth", "Waiting for outputs to initialize");
            }
            else{
                getOutputs().setWheelOutput(left, right);
                Log.i("BackAndForth", "LeftWheel = " + left + " RightWheel = " + right);
            }
        }
    }
}
