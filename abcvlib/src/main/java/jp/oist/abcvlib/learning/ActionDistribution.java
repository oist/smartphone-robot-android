package jp.oist.abcvlib.learning;

import android.util.Log;

// TODO weights and selectedAction should be private to this, then an instance of this class should be global to ClapLearn
public class ActionDistribution{

    private double[] weights = {0.34, 0.34, 0.34};
    private double[] qValues = {0.1,0.1,0.1};
    private double beta = 1.0;
    private double qValuesSum; // allotting memory for this as no numpy type array calcs available?
    private int selectedAction;

    public ActionDistribution(){}

    public ActionDistribution(double[] weights, double[] qValues, double beta){
        this.weights = weights;
        this.qValues = qValues;
        this.beta = beta;
    }

    int actionSelect(){

        double sumOfWeights = 0;
        for (double weight:weights){
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

    public double getBeta(){
        return beta;
    }

    public double getqValuesSum(){
        qValuesSum = 0;
        for (double qValue : qValues) {
            qValuesSum += Math.exp(beta * qValue);
        }
        return qValuesSum;
    }

    public int getSelectedAction() {
        return selectedAction;
    }
}
