package jp.oist.abcvlib.pidbalancer;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.util.ErrorHandler;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity {

    Slider setPoint_;
    Slider p_tilt_;
    Slider d_tilt_;
    Slider p_wheel_;
    Slider expWeight_;
    Slider maxAbsTilt_;
    private String TAG = getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        ArrayList<Slider> controls = new ArrayList<Slider>();

        controls.add(setPoint_ = findViewById(R.id.seekBarSetPoint));
        controls.add(p_tilt_ = findViewById(R.id.seekBarTiltP));
        controls.add(d_tilt_ = findViewById(R.id.seekBarTiltD));
        controls.add(p_wheel_ = findViewById(R.id.seekBarWheelSpeedP));
        controls.add(expWeight_ = findViewById(R.id.seekBarExponentialWeight));
        controls.add(maxAbsTilt_ = findViewById(R.id.seekBarMaxAbsTilt));

        for (Slider slider: controls) {
            slider.addOnChangeListener(sliderChangeListener);
            slider.setLabelFormatter(new LabelFormatter(){
                @NonNull
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.3f", value);
                }
            });
        }

        // Various switches are available to turn on/off core functionality.
        getSwitches().balanceApp = true;
        getSwitches().pythonControlledPIDBalancer = true;
        getSwitches().wheelPolaritySwap = false;

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    private Slider.OnChangeListener sliderChangeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
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
    };
}
