package jp.oist.abcvlib.basic;

import android.app.Activity;
import android.widget.TextView;

import java.text.DecimalFormat;

import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;

public class GuiUpdater implements Runnable{
    private final Activity activity;
    private final TextView voltageBattText;
    private final TextView voltageChargerText;
    private final TextView tiltAngleText;
    private final TextView angularVelocityText;
    private final TextView leftWheelText;
    private final TextView rightWheelText;
    private final TextView soundDataText;
    private final TextView frameRateText;
    private final DecimalFormat df = new DecimalFormat("#.00");
    volatile double batteryVoltage;
    volatile double chargerVoltage;
    volatile double thetaDeg;
    volatile double angularVelocityDeg;
    volatile WheelData wheelData;
    volatile String audioDataString;
    volatile String frameRateString;

    public GuiUpdater(Activity activity){
        this.activity = activity;
        voltageBattText = activity.findViewById(R.id.voltageBattLevel);
        voltageChargerText = activity.findViewById(R.id.voltageChargerLevel);
        tiltAngleText = activity.findViewById(R.id.tiltAngle);
        angularVelocityText = activity.findViewById(R.id.angularVelcoity);
        leftWheelText = activity.findViewById(R.id.leftWheelCount);
        rightWheelText = activity.findViewById(R.id.rightWheelCount);
        soundDataText = activity.findViewById(R.id.soundData);
        frameRateText = activity.findViewById(R.id.frameRate);
    }

    @Override
    public void run() {
        activity.runOnUiThread(() -> {
            voltageBattText.setText(df.format(batteryVoltage));
            voltageChargerText.setText(df.format(chargerVoltage));
            tiltAngleText.setText(df.format(thetaDeg));
            angularVelocityText.setText(df.format(angularVelocityDeg));
            String left = df.format(wheelData.getWheelCountL()) + " : " +
                    df.format(wheelData.getDistanceL()) + " : " +
                    df.format(wheelData.getWheelSpeedL_LP());
            String right = df.format(wheelData.getWheelCountR()) + " : " +
                    df.format(wheelData.getDistanceR()) + " : " +
                    df.format(wheelData.getWheelSpeedR_LP());
            leftWheelText.setText(left);
            rightWheelText.setText(right);
            soundDataText.setText(audioDataString);
            frameRateText.setText(frameRateString);
        });
    }
}
