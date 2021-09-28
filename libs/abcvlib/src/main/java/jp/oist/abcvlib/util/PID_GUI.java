package jp.oist.abcvlib.util;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import jp.oist.abcvlib.core.R;
import jp.oist.abcvlib.tests.BalancePIDController;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PID_GUI#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PID_GUI extends Fragment{

    Slider setPoint_;
    Slider p_tilt_;
    Slider d_tilt_;
    Slider p_wheel_;
    Slider expWeight_;
    Slider maxAbsTilt_;
    private BalancePIDController balancePIDController;
    private final Slider.OnChangeListener sliderChangeListener = (slider, value, fromUser) -> updatePID();

    private String TAG = this.getClass().toString();

    Map<String, Slider> controls = new HashMap<String, Slider>();

    private boolean isQRCodeDisplayed = false;

    public PID_GUI() {
        // Required empty public constructor
    }

    public PID_GUI(BalancePIDController balancePIDController) {
        this.balancePIDController = balancePIDController;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param balancePIDController
     * @return A new instance of fragment pid_gui.
     */
    // TODO: Rename and change types and number of parameters
    public static PID_GUI newInstance(BalancePIDController balancePIDController) {
        return new PID_GUI(balancePIDController);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_pid_gui, container, false);

        controls.put("sp", setPoint_ = rootView.findViewById(R.id.seekBarSetPoint));
        controls.put("pt", p_tilt_ = rootView.findViewById(R.id.seekBarTiltP));
        controls.put("dt", d_tilt_ = rootView.findViewById(R.id.seekBarTiltD));
        controls.put("pw", p_wheel_ = rootView.findViewById(R.id.seekBarWheelSpeedP));
        controls.put("ew", expWeight_ = rootView.findViewById(R.id.seekBarExponentialWeight));
        controls.put("mt", maxAbsTilt_ = rootView.findViewById(R.id.seekBarMaxAbsTilt));

        for (Map.Entry<String, Slider> entry : controls.entrySet()) {
            entry.getValue().addOnChangeListener(sliderChangeListener);
        }
        // Inflate the layout for this fragment

        return rootView;
    }

    public String getControls(){

        JSONObject controlValues = new JSONObject();
        Map<String, Double> controls = new HashMap<String, Double>();

        controls.put("sp", 2.8);
        controls.put("pt", -24.0);
        controls.put("dt", 1.0);
        controls.put("pw", 0.4);
        controls.put("ew", 0.25);
        controls.put("mt", 6.5);

        // Take the value from each slider and store it in a new HashMap
        for (Map.Entry<String, Double> entry : controls.entrySet()) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            String value = df.format(entry.getValue());
            try {
                controlValues.put(entry.getKey(), value);
            } catch (JSONException e) {
                ErrorHandler.eLog(TAG, "Error when processing hashmap for controls", e, true);
            }
        }
        return controlValues.toString();
    };
}