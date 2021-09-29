package jp.oist.abcvlib.pidtransfer_transmitter;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.util.PID_GUI;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    private Button showQRCode;
    private boolean isQRDisplayed = false;
    private PID_GUI pid_view;
    private FragmentManager fragmentManager;
    private CameraPreviewFragment cameraPreviewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        Objects.requireNonNull(getSupportActionBar()).hide();

        showQRCode = findViewById(R.id.show_qr_button);
        showQRCode.setOnClickListener(qrCodeButtonClickListener);

        setIoReadyListener(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onIOReady(AbcvlibLooper abcvlibLooper) {
        // Create your data publisher objects
        PublisherManager publisherManager = new PublisherManager();
        OrientationData orientationData = new OrientationData
                .Builder(this, publisherManager).build();
        WheelData wheelData = new WheelData
                .Builder(this, publisherManager, abcvlibLooper).build();
        // Initialize all publishers (i.e. start their threads and data streams)
        publisherManager.initializePublishers();

        // Create your controllers/subscribers
        balancePIDController = (BalancePIDController) new BalancePIDController().setInitDelay(0)
                .setName("BalancePIDController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5)
                .setTimeUnit(TimeUnit.MILLISECONDS);
        CustomController customController = (CustomController) new CustomController().setInitDelay(0)
                .setName("CustomController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(1000)
                .setTimeUnit(TimeUnit.MILLISECONDS);

        // Attach the controller/subscriber to the publishers
        orientationData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(customController);

        // Start passing data from publishers to subscribers
        publisherManager.startPublishers();

        // Starting and never stopping the customController to see difference between this and adding the PID controller to it via the GUI button.
        customController.startController();
        balancePIDController.startController();

        // Adds your custom controller to the compounding master controller.
        getOutputs().getMasterController().addController(balancePIDController);
        getOutputs().getMasterController().addController(customController);
        // Start the master controller after adding and starting any customer controllers.
        getOutputs().startMasterController();

        runOnUiThread(this::displayPID_GUI);
    }

    private final View.OnClickListener qrCodeButtonClickListener = v -> {
        if (!isQRDisplayed) {
            QRCodeDisplay qrCodeDisplay = QRCodeDisplay.newInstance(pid_view);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.main_fragment, qrCodeDisplay).commit();
            showQRCode.setText(R.string.back_button);
            isQRDisplayed = true;
        } else {
            displayPID_GUI();
        }

    };

    public void displayPID_GUI(){
        pid_view = PID_GUI.newInstance(balancePIDController);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment, pid_view).commit();
        showQRCode.setText(R.string.qr_button_show);
        isQRDisplayed = false;
    }
}
