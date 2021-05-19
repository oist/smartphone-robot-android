package jp.oist.abcvlib.serverlearning;

import jp.oist.abcvlib.core.learning.ActionSet;
import jp.oist.abcvlib.core.learning.CommAction;
import jp.oist.abcvlib.core.learning.CommActionSet;
import jp.oist.abcvlib.core.learning.MotionAction;
import jp.oist.abcvlib.core.learning.MotionActionSet;
import jp.oist.abcvlib.core.learning.StepHandler;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public class MyStepHandler implements StepHandler {

    private final int maxTimeStepCount;
    private boolean lastEpisode = false; // Use to trigger MainActivity to stop generating episodes
    private boolean lastTimestep = false; // Use to trigger MainActivity to stop generating timesteps for a single episode
    private int reward = 0;
    private final int rewardCriterion;
    private final int maxEpisodecount;
    private int episodeCount = 0;
    private final CommActionSet commActionSet;
    private final MotionActionSet motionActionSet;

    public MyStepHandler(int maxTimeStepCount, int rewardCriterion, int maxEpisodeCount,
                         CommActionSet commActionSet, MotionActionSet motionActionSet){
        this.maxTimeStepCount = maxTimeStepCount;
        this.rewardCriterion = rewardCriterion;
        this.maxEpisodecount = maxEpisodeCount;
        this.motionActionSet = motionActionSet;
        this.commActionSet = commActionSet;
    }

    public ActionSet forward(TimeStepDataBuffer.TimeStepData data, int timeStepCount){

        ActionSet actionSet;
        MotionAction motionAction;
        CommAction commAction;

        // Do something with timeStepData... and modify reward accordingly
        reward++;

        // Set actions based on above results. e.g: the first index of each
        motionAction = motionActionSet.getMotionActions()[0];
        commAction = commActionSet.getCommActions()[0];

        // Bundle them into ActionSet so it can return both
        actionSet = new ActionSet(motionAction, commAction);

        // set your action to some ints
        data.getActions().add(motionAction, commAction);

        if (timeStepCount >= maxTimeStepCount || (reward >= rewardCriterion)){
            this.lastTimestep = true;
            episodeCount++;
            // reseting reward after each episode
            reward = 0;
        }

        // todo change criteria to something meaningful
        if (episodeCount >= maxEpisodecount){
            this.lastEpisode = true;
        }

        return actionSet;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public boolean isLastEpisode() {
        return lastEpisode;
    }

    public boolean isLastTimestep() {
        return lastTimestep;
    }

    public int getMaxEpisodecount() {
        return maxEpisodecount;
    }

    public int getMaxTimeStepCount() {
        return maxTimeStepCount;
    }

    public int getReward() {
        return reward;
    }

    public int getRewardCriterion() {
        return rewardCriterion;
    }

    public void setLastTimestep(boolean lastTimestep) {
        this.lastTimestep = lastTimestep;
    }
}
