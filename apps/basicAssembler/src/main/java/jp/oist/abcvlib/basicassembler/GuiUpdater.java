package jp.oist.abcvlib.basicassembler;

import android.app.Activity;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Arrays;

import jp.oist.abcvlib.core.inputs.TimeStepDataBuffer;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;

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
    volatile String episode = "";
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

    private final int maxTimeStepCount;
    private final int maxEpisodeCount;

    public GuiUpdater(Activity activity, int maxTimeStepCount, int maxEpisodeCount){
        this.activity = activity;
        this.maxTimeStepCount = maxTimeStepCount;
        this.maxEpisodeCount = maxEpisodeCount;
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
            episodeText.setText(episode);
            voltageBattText.setText(df.format(batteryVoltage));
            voltageChargerText.setText(df.format(chargerVoltage));
            coilVoltageText.setText(df.format(coilVoltage));
            tiltAngleText.setText(df.format(thetaDeg));
            angularVelocityText.setText(df.format(angularVelocityDeg));
            String left = df.format(wheelCountL) + " : " +
                    df.format(wheelDistanceL) + " : " +
                    df.format(wheelSpeedInstantL) + " : " +
                    df.format(wheelSpeedBufferedL) + " : " +
                    df.format(wheelSpeedExpAvgL);
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

    protected void updateGUIValues(TimeStepDataBuffer.TimeStepData data, int timeStepCount, int episodeCount){
        if (timeStepCount <= maxTimeStepCount){
            timeStep = (timeStepCount + 1) + " of " + maxTimeStepCount;
        }
        if (episodeCount <= maxEpisodeCount){
            episode = (episodeCount + 1) + " of " + maxEpisodeCount;
        }
        if (data.getBatteryData().getVoltage().length > 0){
            batteryVoltage = data.getBatteryData().getVoltage()[0]; // just taking the first recorded one
        }
        if (data.getChargerData().getChargerVoltage().length > 0){
            chargerVoltage = data.getChargerData().getChargerVoltage()[0];
            coilVoltage = data.getChargerData().getCoilVoltage()[0];
        }
        if (data.getOrientationData().getTiltAngle().length > 20){
            thetaDeg = OrientationData.getThetaDeg(data.getOrientationData().getTiltAngle()[0]);
            angularVelocityDeg = OrientationData.getAngularVelocityDeg(data.getOrientationData().getAngularVelocity()[0]);
        }
        if (data.getWheelData().getLeft().getCounts().length > 0){
            wheelCountL = data.getWheelData().getLeft().getCounts()[0];
            wheelCountR = data.getWheelData().getRight().getCounts()[0];
            wheelDistanceL = data.getWheelData().getLeft().getDistances()[0];
            wheelDistanceR = data.getWheelData().getRight().getDistances()[0];
            wheelSpeedInstantL = data.getWheelData().getLeft().getSpeedsInstantaneous()[0];
            wheelSpeedInstantR = data.getWheelData().getRight().getSpeedsInstantaneous()[0];
            wheelSpeedBufferedL = data.getWheelData().getLeft().getSpeedsBuffered()[0];
            wheelSpeedBufferedR = data.getWheelData().getRight().getSpeedsBuffered()[0];
            wheelSpeedExpAvgL = data.getWheelData().getLeft().getSpeedsExpAvg()[0];
            wheelSpeedExpAvgR = data.getWheelData().getRight().getSpeedsExpAvg()[0];
        }
        if (data.getSoundData().getLevels().length > 0){
            float[] arraySlice = Arrays.copyOfRange(data.getSoundData().getLevels(), 0, 5);
            DecimalFormat df = new DecimalFormat("0.#E0");
            String arraySliceString = "";
            for (double v : arraySlice) {
                arraySliceString = arraySliceString.concat(df.format(v)) + ", ";
            }
            audioDataString = arraySliceString;
        }
        if (data.getImageData().getImages().size() > 1){
            double frameRate = 1.0 / ((data.getImageData().getImages().get(1).getTimestamp() -
                    data.getImageData().getImages().get(0).getTimestamp()) / 1000000000.0) ; // just taking difference between two but one could do an average over all differences
            DecimalFormat df = new DecimalFormat("#.0000000000000");
            frameRateString = df.format(frameRate);
        }
    }
}
