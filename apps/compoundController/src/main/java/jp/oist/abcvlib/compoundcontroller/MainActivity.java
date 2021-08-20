package jp.oist.abcvlib.compoundcontroller;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataListener;
import jp.oist.abcvlib.core.outputs.AbcvlibController;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.ScheduledExecutorServiceWithException;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * Shows how to setup custom controller in conjunction with the the PID balance controller.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    ScheduledExecutorServiceWithException executor;
    private Slider setPoint_;
    private Slider p_tilt_;
    private Slider d_tilt_;
    private Slider p_wheel_;
    private Slider expWeight_;
    private Slider maxAbsTilt_;
    private final String TAG = getClass().getName();

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

        // Various switches are available to turn on/off core functionality.
        getSwitches().balanceApp = true;

        // Informs AbcvlibActivity that this is the class it should call when IO is ready.
        setIoReadyListener(this);

        executor = new ScheduledExecutorServiceWithException(1, new ProcessPriorityThreadFactory(Thread.MIN_PRIORITY, "compoundController"));

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    private Slider.OnChangeListener sliderChangeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            updatePID();
        }
    };

    private void updatePID(){
        try {
            getOutputs().getBalancePIDController().setPID(p_tilt_.getValue(),
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
        if (button.getText() == "Start"){
            // Sets initial values rather than wait for slider change
            updatePID();
            button.setText("Stop");
            getOutputs().getBalancePIDController().startController();

        }else{
            button.setText("Start");
            getOutputs().getBalancePIDController().stop();
            getOutputs().setWheelOutput(0,0);
        }
    }

    @Override
    public void onIOReady() {
//        CustomController customController = new CustomController();
//
//        // connect the customConroller as a subscriber to wheel data updates
//        getInputs().getWheelData().setWheelDataListener(customController);
//
//        // Add the custom controller to the grand controller (controller that assembles other controllers)
//        getOutputs().getMasterController().addController(customController);
//
//        executor.scheduleAtFixedRate(customController, 0, 100, TimeUnit.MILLISECONDS);
        getOutputs().getBalancePIDController().startController();
    }

    /**
     * Simple proportional controller trying to achieve some setSpeed set by python server GUI.
     */
    public static class CustomController extends AbcvlibController implements WheelDataListener {

        double actualSpeed = 0;
        double errorSpeed = 0;
        double maxSpeed = 350; // Just spot measurede this by setOutput(1.0, 1.0) and read the log. This will surely change with battery level and wheel wear/tear.

        double setSpeed = 100; // mm/s.
        double d_s = 0.1; // derivative controller for speed of wheels

        public void run(){
            errorSpeed = setSpeed - actualSpeed;

            // Note the use of the same output for controlling both wheels. Due to various errors
            // that build up over time, controling individual wheels has so far led to chaos
            // and unstable controllers.
            setOutput((float) ((setSpeed / maxSpeed) + ((errorSpeed * d_s) / maxSpeed)), (float) ((setSpeed / maxSpeed) + ((errorSpeed * d_s) / maxSpeed)));
        }

        @Override
        public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR, double wheelDistanceL, double wheelDistanceR, double wheelSpeedInstantL, double wheelSpeedInstantR, double wheelSpeedBufferedL, double wheelSpeedBufferedR, double wheelSpeedExpAvgL, double wheelSpeedExpAvgR) {
            actualSpeed = wheelSpeedBufferedL;
            Log.d("WheelUpdate", "wheelSpeedBufferedL: " + wheelSpeedBufferedL + ", wheelSpeedBufferedR: " + wheelSpeedBufferedR);
        }
    }
}
