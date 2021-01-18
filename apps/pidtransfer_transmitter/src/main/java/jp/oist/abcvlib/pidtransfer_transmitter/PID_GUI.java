package jp.oist.abcvlib.pidtransfer_transmitter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import jp.oist.abcvlib.core.outputs.Outputs;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PID_GUI#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PID_GUI extends Fragment {

    Slider setPoint_;
    Slider p_tilt_;
    Slider d_tilt_;
    Slider p_wheel_;
    Slider expWeight_;
    Slider maxAbsTilt_;

    Map<String, Slider> controls = new HashMap<String, Slider>();

    private Outputs outputs;
    private boolean isQRCodeDisplayed = false;

    public PID_GUI() {
        // Required empty public constructor
    }

    public PID_GUI(Outputs outputs) {
        this.outputs = outputs;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param outputs Outputs for PID controller
     * @return A new instance of fragment pid_gui.
     */
    // TODO: Rename and change types and number of parameters
    public static PID_GUI newInstance(Outputs outputs) {
        return new PID_GUI(outputs);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private final Slider.OnChangeListener sliderChangeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            try {
                outputs.balancePIDController.setPID(p_tilt_.getValue(),
                        0,
                        d_tilt_.getValue(),
                        setPoint_.getValue(),
                        p_wheel_.getValue(),
                        expWeight_.getValue(),
                        maxAbsTilt_.getValue());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

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
        // Take the value from each slider and store it in a new HashMap
        for (Map.Entry<String, Slider> entry : controls.entrySet()) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            String value = df.format(entry.getValue().getValue());
            try {
                controlValues.put(entry.getKey(), value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return controlValues.toString();
    };
}