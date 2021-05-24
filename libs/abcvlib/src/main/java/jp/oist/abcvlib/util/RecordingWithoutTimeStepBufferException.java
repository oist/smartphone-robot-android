package jp.oist.abcvlib.util;

public class RecordingWithoutTimeStepBufferException extends Exception{
    Exception exception = null;
    private final String TAG = getClass().getName();
    public RecordingWithoutTimeStepBufferException(){
        exception = new Exception("Trying to set recording to true prior to initializing a TimeStepDataBuffer");
    }
}
