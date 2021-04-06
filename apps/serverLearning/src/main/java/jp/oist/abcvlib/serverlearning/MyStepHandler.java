package jp.oist.abcvlib.serverlearning;

public class MyStepHandler{

    private TimeStepDataBuffer.TimeStepData timeStepData;
    private int maxTimeStepCount;

    public MyStepHandler(TimeStepDataBuffer.TimeStepData data, int maxTimeStepCount){
        this.timeStepData = data;
        this.maxTimeStepCount = maxTimeStepCount;
    }

    public boolean foward(int timeStepCount){

        // set to true if episode should end after this timestep
        boolean endEpisode = false;

        int motionAction = 0;
        int commAction = 0;

        // Do something with timeStepData...

        // set your action to some ints
        timeStepData.actions.add(motionAction, commAction);

        if (timeStepCount >= maxTimeStepCount){
            endEpisode = true;
        }

        return endEpisode;
    }

}
