package jp.oist.abcvlib.util;

public class DSP {
    public static double exponentialAvg(double sample, double expAvg, double weighting){
        expAvg = (1.0 - weighting) * expAvg + (weighting * sample);
        return expAvg;
    }
}
