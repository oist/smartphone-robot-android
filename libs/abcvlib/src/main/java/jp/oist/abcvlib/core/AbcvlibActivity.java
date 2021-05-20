package jp.oist.abcvlib.core;

import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import jp.oist.abcvlib.core.inputs.Inputs;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataGatherer;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataGatherer;
import jp.oist.abcvlib.core.inputs.phone.vision.ImageAnalyzerActivity;
import jp.oist.abcvlib.core.learning.ActionDistribution;
import jp.oist.abcvlib.core.learning.ActionSelector;
import jp.oist.abcvlib.core.learning.RewardGenerator;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;
import jp.oist.abcvlib.core.outputs.AbcvlibController;
import jp.oist.abcvlib.core.outputs.Outputs;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.SocketListener;

/**
 * AbcvlibActivity is where all of the other classes are initialized into objects. The objects
 * are then passed to one another in order to coordinate the various shared values between them.
 *
 * Android app MainActivity can start Motion by extending AbcvlibActivity and then running
 * any of the methods within the object instance Motion within an infinite threaded loop
 * e.g:
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 *
 */
public abstract class AbcvlibActivity extends IOIOActivity implements RewardGenerator,
        OnRecordPositionUpdateListener, ImageAnalyzerActivity, SocketListener{

    // Publically accessible objects that encapsulate a lot other core functionality
    public Inputs inputs;
    public Outputs outputs;
    public ActionDistribution aD;
    public ActionSelector aS;
    private Thread actionSelectorThread;
    public Switches switches = new Switches();
    public ExecutorService audioExecutor;
    private AbcvlibActivity mainActivity;
    private BatteryDataGatherer batteryDataGatherer;
    private WheelDataGatherer wheelDataGatherer;

    /**
     * Lets various loops know its time to wrap things up when false, and prevents other loops from
     * starting until true. Set to true after AbcvlibActivity.initializer() finishes.
     */
    public boolean appRunning = false;

    // Other generics
    protected static final String TAG = "abcvlib";

    public int avgCount = 1000;
    private static String[] REQUIRED_PERMISSIONS = new String[0];

    protected void onCreate(Bundle savedInstanceState) {
        if(!appRunning){
            throw new IllegalStateException("initialize() not called prior to onCreate()");
        }
        else{
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            audioExecutor = Executors.newScheduledThreadPool(1, new ProcessPriorityThreadFactory(10, "dataGatherer"));
        }

        Log.i(TAG, "End of AbcvlibActivity.onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "End of AbcvlibActivity.onStart");
    }

    @Override
    protected void onStop() {
        appRunning = false;
        super.onStop();
        outputs.motion.setWheelOutput(0, 0);
        Toast.makeText(this, "In onStop", Toast.LENGTH_LONG).show();
        Log.i(TAG, "onStop Log");
        Log.i(TAG, "End of AbcvlibActivity.onStop");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        outputs.motion.setWheelOutput(0, 0);
        Log.i(TAG, "End of AbcvlibActivity.onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "End of AbcvlibActivity.onDestroy");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG, "End of AbcvlibActivity.onResume");
    }

    protected void initialzer(AbcvlibActivity abcvlibActivity,
                              AbcvlibController controller,
                              ImageAnalyzerActivity imageAnalyzerActivity,
                              BatteryDataGatherer batteryDataGatherer,
                              WheelDataGatherer wheelDataGatherer,
                              TimeStepDataBuffer timeStepDataBuffer) {

        //Todo some logic here to test for boolean combinations that would lead to errors.
        // e.g. balanceApp without pythonControlApp

        this.batteryDataGatherer = batteryDataGatherer;
        this.wheelDataGatherer = wheelDataGatherer;

        mainActivity = abcvlibActivity;
        Log.i(TAG, "Start of AbcvlibActivity.initializer");

        inputs = new Inputs(abcvlibActivity, imageAnalyzerActivity, timeStepDataBuffer);
        outputs = new Outputs(abcvlibActivity, controller);

        if (switches.actionSelectorApp){
            if (aD == null){
                aD = new ActionDistribution();
            }
            aS = new ActionSelector(this);
            aS.start();
        }

        // Tell all child classes it is ok to proceed.
        this.appRunning = true;

        abcvlibActivity.onSetupFinished();

        Log.i(TAG, "End of AbcvlibActivity.initializer");
    }

    /**
     * null initializer for basic module or those not interacting with anything other than itself
     */
    protected void initialzer(AbcvlibActivity abcvlibActivity) {

        initialzer(abcvlibActivity, null, null, null, null, null);

    }

    protected void onSetupFinished() {

    }

    public void setRequiredPermissions(String[] permissions){
        for (String permission : permissions){
            REQUIRED_PERMISSIONS = Arrays.copyOf(REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS.length + 1);
            REQUIRED_PERMISSIONS[REQUIRED_PERMISSIONS.length - 1] = permission;
        }
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                inputs.camerax.startCamera();
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    public boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     Overriding here passes the initialized AbcvlibLooper object to the IOIOLooper class which
     then connects the object to the actual IOIOBoard. There is no need to start a separate thread
     or create a global objects in the Android App MainActivity or otherwise.
      */
    @Override
    protected IOIOLooper createIOIOLooper() {
        /*
         This wait loop prevents the creation of AbcvlibLooper before we can assign the values of
         loggerOn and wheelPolaritySwap. Since we have no access to this call within IOIOActivity,
         this is the only way I found possible. Though this may have a better workaround, it should
         only potentially add a few hundred milliseconds to the startup of the app, and have no
         performance degredation while up and running.
         */
//        while (!appRunning){
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                Log.e(TAG,"Error", e);
//            }
//        }
        Log.d("abcvlib", "createIOIOLooper Finished");
        return new AbcvlibLooper(this,
                switches.loggerOn,
                switches.wheelPolaritySwap,
                batteryDataGatherer, wheelDataGatherer);
    }

    public double determineReward(){
        return 0;
    }

    @Override
    public ImageAnalysis getAnalyzer() {
        return null;
    }

    @Override
    public void onMarkerReached(AudioRecord audioRecord) {

    }

    /**
     * This method fires 2 times during each loop of the audio record buffer.
     * audioRecord.read(audioData) writes the buffer values (stored in the audioRecord) to a local
     * float array called audioData. It is set to read in non_blocking mode
     * (https://developer.android.com/reference/android/media/AudioRecord?hl=ja#READ_NON_BLOCKING)
     * You can verify it is not blocking by checking the log for "Missed some audio samples"
     * You can verify if the buffer writer is overflowing by checking the log for:
     * "W/AudioFlinger: RecordThread: buffer overflow"
     * @param audioRecord
     */
    @Override
    public void onPeriodicNotification(AudioRecord audioRecord) {

        audioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                int writeBufferSizeFrames = audioRecord.getBufferSizeInFrames();
                int readBufferSize = audioRecord.getPositionNotificationPeriod();
//                Log.d("microphone", "readBufferSize:" + readBufferSize);
                float[] audioData = new float[readBufferSize];
                int numSamples = audioRecord.read(audioData, 0,
                        readBufferSize, AudioRecord.READ_NON_BLOCKING);
//                Log.d("microphone", "numSamples:" + numSamples);
                if (numSamples < readBufferSize){
                    Log.w("microphone", "Missed some audio samples");
                }
//                Log.v("microphone", numSamples + " / " + writeBufferSizeFrames + " samples read");
                mainActivity.onNewAudioData(audioData, numSamples);
            }
        });
    }

    /**
     * Interface type method used by onPeriodicNotification to send audioData to the main activity
     * object for custom processing. This allows the buffer size checking and threading to be
     * abstracted to this AbcvlibAcvitiy.
     * @param audioData
     */
    protected void onNewAudioData(float[] audioData, int numSamples){
    }

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse Message from Server
        Log.i("server", "Reading from Server Completed");
    }

}
