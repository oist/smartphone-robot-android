package jp.oist.abcvlib.serverlearning;

public class MyStepHandler{

    private TimeStepDataBuffer.TimeStepData timeStepData;

    public MyStepHandler(TimeStepDataBuffer.TimeStepData data){
        this.timeStepData = data;
    }

    public void foward(){

        int motionAction = 0;
        int commAction = 0;

        // Do something with timeStepData...

        // set your action to some ints
        timeStepData.actions.add(motionAction, commAction);
    }

}
