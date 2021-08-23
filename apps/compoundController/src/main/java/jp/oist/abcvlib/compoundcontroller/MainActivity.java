package jp.oist.abcvlib.compoundcontroller;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.util.ErrorHandler;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * Shows how to setup custom controller in conjunction with the the PID balance controller.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    private Slider setPoint_;
    private Slider p_tilt_;
    private Slider d_tilt_;
    private Slider p_wheel_;
    private Slider expWeight_;
    private Slider maxAbsTilt_;
    private final String TAG = getClass().getName();
    private BalancePIDController balancePIDController;
    private final Slider.OnChangeListener sliderChangeListener = (slider, value, fromUser) -> updatePID();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        ArrayList<Slider> controls = new ArrayList<>();

        controls.add(setPoint_ = findViewById(R.id.seekBarSetPoint));
        controls.add(p_tilt_ = findViewById(R.id.seekBarTiltP));
        controls.add(d_tilt_ = findViewById(R.id.seekBarTiltD));
        controls.add(p_wheel_ = findViewById(R.id.seekBarWheelSpeedP));
        controls.add(expWeight_ = findViewById(R.id.seekBarExponentialWeight));
        controls.add(maxAbsTilt_ = findViewById(R.id.seekBarMaxAbsTilt));

        for (Slider slider: controls) {
            slider.addOnChangeListener(sliderChangeListener);
            slider.setLabelFormatter(value -> String.format(Locale.JAPAN, "%.3f", value));
        }

        // Informs AbcvlibActivity that this is the class it should call when IO is ready.
        setIoReadyListener(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    private void updatePID(){
        try {
            balancePIDController.setPID(p_tilt_.getValue(),
                    0,
                    d_tilt_.getValue(),
                    setPoint_.getValue(),
                    p_wheel_.getValue(),
                    expWeight_.getValue(),
                    maxAbsTilt_.getValue());
        } catch (InterruptedException e) {
            ErrorHandler.eLog(TAG, "Error when getting slider gui values", e, true);
        }
    }

    public void buttonClick(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Start")){
            // Sets initial values rather than wait for slider change
            updatePID();
            button.setText("Stop");
            balancePIDController.startController();

        }else{
            button.setText("Start");
            balancePIDController.stopController();
        }
    }

    @Override
    public void onIOReady() {
        balancePIDController = (BalancePIDController) new BalancePIDController(getInputs()).setInitDelay(0)
                .setName("BalancePIDController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5)
                .setTimeUnit(TimeUnit.MILLISECONDS);

        CustomController customController = (CustomController) new CustomController(getInputs()).setInitDelay(0)
                .setName("CustomController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(1000)
                .setTimeUnit(TimeUnit.MILLISECONDS);

        // Starting and never stopping the customController to see difference between this and adding the PID controller to it via the GUI button.
        customController.startController();

        // Adds your custom controller to the compounding master controller.
        getOutputs().getMasterController().addController(balancePIDController);
        getOutputs().getMasterController().addController(customController);
        // Start the master controller after adding and starting any customer controllers.
        getOutputs().startMasterController();
    }
}
