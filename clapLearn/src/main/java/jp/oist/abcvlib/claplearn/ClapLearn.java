package jp.oist.abcvlib.claplearn;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.learning.ActionDistribution;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs basic Q-learning demo with reward based on 5s average microphone levels.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class ClapLearn extends AbcvlibActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        double[] weights = {0.34, 0.34, 0.34};
        double[] qValues = {0.1, 0.1, 0.1};
        double learningRate = 0.1;
        double temperature = 5.0;
        aD = new ActionDistribution(weights, qValues, learningRate, temperature);

        switches.pythonControlApp = true;
        switches.micApp = true;
        switches.actionSelectorApp = true;

        // Passes Android App information up to parent classes for various usages.
        initialzer(this, "192.168.20.26", 65434);

        // Read the layout and construct.
        setContentView(R.layout.main);

        // Passes states up to Android Activity. Do not modify
        super.onCreate(savedInstanceState);
    }

    @Override
    public synchronized double determineReward(){
        // Choose action based on current probabilities.
        int selectedAction = aD.actionSelect();

        switch (selectedAction){
            case 0:
                Log.i("abcvlib", "Selected Action 0");
                break;
            case 1:
                Log.i("abcvlib", "Selected Action 1");
                break;
            case 2:
                Log.i("abcvlib", "Selected Action 2");
                break;
            default:
                Log.i("distribution", "default case selected with action = " + selectedAction);
        }

        double reward = calcReward();

        aD.setReward(reward);
        Log.i("abcvlib", "reward:" + reward);

        aD.updateValues(reward, selectedAction);
        Log.i("abcvlib", "updateValues");

        return reward;
    }

    private double calcReward(){
        // Update your reward however you please.
        long startTime = System.nanoTime();
        int iterations = 0;
        double reward = 0;
        double timeInterval = 5000000000.0; // 5 seconds in nanoseconds

        // add up levels for timeInterval then divide by total iterations to find average
        while (System.nanoTime() <= (startTime + timeInterval)){
            // 35 dB is appx the ambient noise level in the room. Trying to make ambient -> 0.
            double normalizedLevel = ((inputs.micInput.getRmsdB() / 35) - 1);
            reward = reward + normalizedLevel;
            iterations++;
            Thread.yield();
        }
        return (reward / iterations);
    }

}
