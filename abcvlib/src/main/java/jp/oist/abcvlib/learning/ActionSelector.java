package jp.oist.abcvlib.learning;


import android.util.Log;

import jp.oist.abcvlib.AbcvlibActivity;

public class ActionSelector implements Runnable{

    int speed = 100; // Duty Cycle from 0 to 100.
    long actionCycle = 5000; // How long each action is carried out in ms
    int selectedAction;
    boolean appRunning = false;
    ActionDistribution aD;
    // Q-learning Variables
    final double alpha = 0.1; // learning rate
    final double beta = 3.0; // temperature
    private double reward; // reward based on spl
    double rewardSum = 0;
    private double rewardAvg = 0;
    int ellapsedLoops = 1; // For use in the moving average of the reward.
    double rewardScaleFactor = (100.0); // (mRmsSmoothed / rewardScaleFactor) - rewardOffset
    double rewardOffset = 1.5;
    private double rmsdB; // rmsdB value each loop. Written by processAudioFrame read by separate thread in ActionSelector

    public ActionSelector(AbcvlibActivity activity){
        this.appRunning = true;
        this.aD = new ActionDistribution();
    }

    public void run() {
        while(appRunning) {
            selectedAction = aD.actionSelect();

            switch (selectedAction){
                case 0:
                    // Do something
                    break;
                case 1:
                    // Do something
                    break;
                case 2:
                    // Do something
                    break;
                default:
                    Log.i("distribution", "default case selected with action = " + selectedAction);
            }
            updateValues();
        }
    }

    private void updateValues(){
        // update qValues due to current level
        aD.getqValues()[aD.getSelectedAction()] = (alpha * rewardAvg) + ((1.0 - alpha) * aD.getqValues()[aD.getSelectedAction()]);
        // update weights from new qValues
        aD.getqValuesSum();
        for (int i=0; i < aD.getWeights().length; i++){
            aD.getWeights()[i] = Math.exp(beta * aD.getqValues()[i]) / aD.getqValuesSum();
        }
        reward = -rewardOffset;
        rewardAvg = 0;
        rewardSum = 0;
        rmsdB = 0;
        ellapsedLoops = 1;
    }

    public double getReward(){
        return reward;
    }

    public double getRewardAvg(){
        return rewardAvg;
    }
}

