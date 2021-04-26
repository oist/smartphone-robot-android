package jp.oist.abcvlib.serverlearning;

public class MyStepHandler{

    private TimeStepDataBuffer.TimeStepData timeStepData;
    private int maxTimeStepCount;
    private boolean lastEpisode = false; // Use to trigger MainActivity to stop generating episodes
    private boolean lastTimestep = false; // Use to trigger MainActivity to stop generating timesteps for a single episode

    public MyStepHandler(TimeStepDataBuffer.TimeStepData data, int maxTimeStepCount){
        this.timeStepData = data;
        this.maxTimeStepCount = maxTimeStepCount;
    }

    public ActionSet foward(int timeStepCount){

        ActionSet actionSet;
        MotionAction motionAction;
        CommAction commAction;

        // Do something with timeStepData...

        // Set actions based on above results. e.g:
        motionAction = MotionAction.FORWARD;
        commAction = CommAction.COMM_ACTION1;

        // Bundle them into ActionSet so it can return both
        actionSet = new ActionSet(motionAction, commAction);

        // set your action to some ints
        timeStepData.actions.add(motionAction, commAction);

        if (timeStepCount >= maxTimeStepCount){
            this.lastTimestep = true;
        }

        return actionSet;
    }

}
