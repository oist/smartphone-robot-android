package jp.oist.abcvlib.basicassembler;

import android.app.Activity;
import android.widget.TextView;

import java.text.DecimalFormat;

public class GuiUpdater implements Runnable{
    private final Activity activity;
    private final TextView timeStepText;
    private final TextView episodeText;
    private final TextView voltageBattText;
    private final TextView voltageChargerText;
    private final TextView coilVoltageText;
    private final TextView tiltAngleText;
    private final TextView angularVelocityText;
    private final TextView leftWheelText;
    private final TextView rightWheelText;
    private final TextView soundDataText;
    private final TextView frameRateText;
    private final DecimalFormat df = new DecimalFormat("#.00");
    volatile String timeStep = "";
    volatile String episodeCount = "";
    volatile double batteryVoltage = 0.0;
    volatile double chargerVoltage = 0.0;
    volatile double coilVoltage = 0.0;
    volatile double thetaDeg = 0.0;
    volatile double angularVelocityDeg = 0.0;
    volatile int wheelCountL;
    volatile int wheelCountR;
    volatile double wheelDistanceL = 0.0;
    volatile double wheelDistanceR = 0.0;
    volatile double wheelSpeedInstantL = 0.0;
    volatile double wheelSpeedInstantR = 0.0;
    volatile double wheelSpeedBufferedL = 0.0;
    volatile double wheelSpeedBufferedR = 0.0;
    volatile double wheelSpeedExpAvgL = 0.0;
    volatile double wheelSpeedExpAvgR = 0.0;

    volatile String audioDataString = "";
    volatile String frameRateString = "";

    public GuiUpdater(Activity activity){
        this.activity = activity;
        timeStepText = activity.findViewById(R.id.timeStep);
        episodeText = activity.findViewById(R.id.episodeCount);
        voltageBattText = activity.findViewById(R.id.voltageBattLevel);
        voltageChargerText = activity.findViewById(R.id.voltageChargerLevel);
        coilVoltageText = activity.findViewById(R.id.coilVoltageText);
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
            timeStepText.setText(timeStep);
            episodeText.setText(episodeCount);
            voltageBattText.setText(df.format(batteryVoltage));
            voltageChargerText.setText(df.format(chargerVoltage));
            coilVoltageText.setText(df.format(coilVoltage));
            tiltAngleText.setText(df.format(thetaDeg));
            angularVelocityText.setText(df.format(angularVelocityDeg));
            String left = df.format(wheelCountL) + " : " +
                    df.format(wheelDistanceL) + " : " +
                    df.format(wheelSpeedInstantL) + " : " +
                    df.format(wheelSpeedBufferedL) + " : " +
                    df.format(wheelSpeedExpAvgL);;
            String right = df.format(wheelCountR) + " : " +
                    df.format(wheelDistanceR) + " : " +
                    df.format(wheelSpeedInstantR) + " : " +
                    df.format(wheelSpeedBufferedR) + " : " +
                    df.format(wheelSpeedExpAvgR);
            leftWheelText.setText(left);
            rightWheelText.setText(right);
            soundDataText.setText(audioDataString);
            frameRateText.setText(frameRateString);
        });
    }
}
