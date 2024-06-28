package jp.oist.abcvlib.serverlearning;

import android.os.Bundle;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.ImageDataRaw;
import jp.oist.abcvlib.core.inputs.phone.MicrophoneData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.QRCodeData;
import jp.oist.abcvlib.core.learning.ActionSpace;
import jp.oist.abcvlib.core.learning.CommActionSpace;
import jp.oist.abcvlib.core.learning.MetaParameters;
import jp.oist.abcvlib.core.learning.MotionActionSpace;
import jp.oist.abcvlib.core.learning.StateSpace;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.UsbSerial;

public class MainActivity extends AbcvlibActivity implements SerialReadyListener {
    private final int maxEpisodeCount = 3;
    private final int maxTimeStepCount = 40;
    private StateSpace stateSpace;
    private ActionSpace actionSpace;
    TimeStepDataBuffer timeStepDataBuffer;

    InetSocketAddress inetSocketAddress;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    @SuppressWarnings("unused")
    private final String TAG = getClass().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted.
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        // Copies all files from assets/models to local storage
        FileOps.copyAssets(getApplicationContext(), "models/");

        timeStepDataBuffer = new TimeStepDataBuffer(200);

        // Initialize InetSocketAddress in a background thread and wait for completion
        Future<InetSocketAddress> future = executorService.submit(new Callable<InetSocketAddress>() {
            @Override
            public InetSocketAddress call() {
                return new InetSocketAddress(BuildConfig.IP, BuildConfig.PORT);
            }
        });

        try {
            inetSocketAddress = future.get(); // This will wait until inetSocketAddress is initialized
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        /*------------------------------------------------------------------------------
        ------------------------------ Define Action Space -----------------------------
        --------------------------------------------------------------------------------
         */
        // Defining custom actions
        CommActionSpace commActionSpace = new CommActionSpace(3);
        commActionSpace.addCommAction("action1", (byte) 0); // I'm just overwriting an existing to show how
        commActionSpace.addCommAction("action2", (byte) 1);
        commActionSpace.addCommAction("action3", (byte) 2);

        MotionActionSpace motionActionSpace = new MotionActionSpace(5);
        motionActionSpace.addMotionAction("stop", (byte) 0, 0, 0, false, false); // I'm just overwriting an existing to show how
        motionActionSpace.addMotionAction("forward", (byte) 1, 1, 1, false, false);
        motionActionSpace.addMotionAction("backward", (byte) 2, -1, -1, false, false);
        motionActionSpace.addMotionAction("left", (byte) 3, -1, 1, false, false);
        motionActionSpace.addMotionAction("right", (byte) 4, 1, -1, false, false);

        actionSpace = new ActionSpace(commActionSpace, motionActionSpace);

        /*------------------------------------------------------------------------------
        ------------------------------ Define State Space ------------------------------
        --------------------------------------------------------------------------------
         */
        PublisherManager publisherManager = new PublisherManager();
        // Customize how each sensor data is gathered via constructor params or builders
        WheelData wheelData = new WheelData.Builder(this, publisherManager)
                .setBufferLength(50)
                .setExpWeight(0.01)
                .build();
        wheelData.addSubscriber(timeStepDataBuffer);

        BatteryData batteryData = new BatteryData.Builder(this, publisherManager).build();
        batteryData.addSubscriber(timeStepDataBuffer);

        OrientationData orientationData = new OrientationData.Builder(this, publisherManager).build();
        orientationData.addSubscriber(timeStepDataBuffer);

        MicrophoneData microphoneData = new MicrophoneData.Builder(this, publisherManager).build();
        microphoneData.addSubscriber(timeStepDataBuffer);

        ImageDataRaw imageDataRaw = new ImageDataRaw.Builder(this, publisherManager, this)
                .setPreviewView(findViewById(R.id.camera_x_preview)).build();
        imageDataRaw.addSubscriber(timeStepDataBuffer);

        QRCodeData qrCodeData = new QRCodeData.Builder(this, publisherManager, this).build();
        qrCodeData.addSubscriber(timeStepDataBuffer);

        stateSpace = new StateSpace(publisherManager);
        setSerialCommManager(new SerialCommManager(usbSerial, batteryData, wheelData));
        super.onSerialReady(usbSerial);
        /*------------------------------------------------------------------------------
        ----------- Initialize and Start Trial After PublisherManager Ready ----------------------
        --------------------------------------------------------------------------------
         */

    }

    @Override
    protected void onOutputsReady(){
        /*------------------------------------------------------------------------------
        ------------------------------ Set MetaParameters ------------------------------
        --------------------------------------------------------------------------------
         */
        MetaParameters metaParameters = new MetaParameters(this, 50, maxTimeStepCount,
                100000, maxEpisodeCount, inetSocketAddress, timeStepDataBuffer, getOutputs(), 1);

        /*------------------------------------------------------------------------------
        ------------------------------ Initialize and Start Trial ----------------------
        --------------------------------------------------------------------------------
         */
        MyTrial myTrial = new MyTrial(metaParameters, actionSpace, stateSpace);
        myTrial.startTrail();
    }
}