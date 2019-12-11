package jp.oist.abcvlib.learning;

import android.util.Log;

// TODO weights and selectedAction should be private to this, then an instance of this class should be global to ClapLearn
public class ActionDistribution{

    private double reward = 0;
    private double[] weights = {0.34, 0.34, 0.34};
    private double[] qValues = {0.1, 0.1, 0.1};
    private double learningRate = 0.1;
    private double temperature = 1.0;
    private int selectedAction;

    public ActionDistribution(){}

    public ActionDistribution(double[] weights, double[] qValues, double learningRate, double temperature){
        this.weights = weights;
        this.qValues = qValues;
        this.learningRate = learningRate;
        this.temperature = temperature;
    }

    public int actionSelect(){

        double sumOfWeights = 0;
        for (double weight: weights){
            sumOfWeights = sumOfWeights + weight;
        }
        double randNum = Math.random() * sumOfWeights;
        double selector = 0;
        int iterator = -1;

        while (selector < randNum){
            try {
                iterator++;
                selector = selector + weights[iterator];
            }catch (ArrayIndexOutOfBoundsException e){
                Log.e("abcvlib", "weight index bound exceeded. randNum was greater than the sum of all weights. This can happen if the sum of all weights is less than 1.");
            }
        }
        // Assigning this as a read-only value to pass between threads.
        this.selectedAction = iterator;

        // represents the action to be selected
        return iterator;
    }

    public double[] getWeights(){
        return weights;
    }

    public double[] getqValues(){
        return qValues;
    }

    public double getQValue(int action){
        return qValues[action];
    }

    public double getTemperature(){
        return temperature;
    }


    public int getSelectedAction() {
        return selectedAction;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public void setQValue(int action, double qValue) {
        this.qValues[action] = qValue;
    }

    public void updateValues(double reward, int action){

        double qValuePrev = getQValue(action);

        // update qValues due to current reward
        setQValue(action,(learningRate * reward) + ((1.0 - learningRate) * qValuePrev));
        // update weights from new qValues

        double qValuesSum = 0;
        for (double qValue : getqValues()) {
            qValuesSum += Math.exp(temperature * qValue);
        }

        // update weights
        for (int i = 0; i < getWeights().length; i++){
            getWeights()[i] = Math.exp(temperature * getqValues()[i]) / qValuesSum;
        }
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }
}
