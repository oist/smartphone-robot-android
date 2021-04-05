package jp.oist.abcvlib.serverlearning;

public class MyStepHandler implements Runnable{

    private TimeStepDataBuffer.TimeStepData timeStepData;

    public MyStepHandler(TimeStepDataBuffer.TimeStepData data){
        this.timeStepData = data;
    }

    @Override
    public void run() {

    }
}
