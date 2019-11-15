package jp.oist.abcvlib.claplearn;

public class Buzzer extends Object {

//    double duration = 1;
//    double frequency = 1000;
//    int sampleRate = 8000;
//
//    double dnumSamples;
//    int numSamples;
//    double sample[];
//    byte generatedSnd[];
//
//    public Buzzer(){
//
//        this(1, 1000, 8000);
//
//    }
//
//    public Buzzer (double duration, double frequency, int sampleRate) {
//
//        this.duration = duration;            // seconds
//        this.frequency = 1000;       // hz
//        this.sampleRate = 8000;          // a number
//
//        this.dnumSamples = duration * sampleRate;
//        this.dnumSamples = Math.ceil(dnumSamples);
//        this.numSamples = (int) dnumSamples;
//        this.sample = new double[numSamples];
//        this.generatedSnd = new byte[2 * numSamples];
//
//    }
//
//    public start(){
//
//    }
//
//    public stop(){
//
//    }
//    for (int i = 0; i < numSamples; ++i) {    // Fill the sample array
//            sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
//        }
//
//        // convert to 16 bit pcm sound array
//    // assumes the sample buffer is normalized.
//    // convert to 16 bit pcm sound array
//    // assumes the sample buffer is normalised.
//        int idx = 0;
//        int i = 0 ;
//
//        int ramp = numSamples / 20 ;                                     // Amplitude ramp as a percent of sample count
//
//
//    for (i = 0; i< ramp; ++i) {                                      // Ramp amplitude up (to avoid clicks)
//            double dVal = sample[i];
//            // Ramp up to maximum
//            final short val = (short) ((dVal * 32767 * i/ramp));
//            // in 16 bit wav PCM, first byte is the low order byte
//            generatedSnd[idx++] = (byte) (val & 0x00ff);
//            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
//        }
}


