package jp.oist.abcvlib.core.learning;

public class ActionSpace {
    public final CommActionSet commActionSet;
    public final MotionActionSet motionActionSet;
    public ActionSpace(CommActionSet commActionSet, MotionActionSet motionActionSet){
        this.commActionSet = commActionSet;
        this.motionActionSet = motionActionSet;
    }
}
