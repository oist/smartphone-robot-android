package jp.oist.abcvlib.core.learning;

public class ActionSpace {
    public final CommActionSpace commActionSpace;
    public final MotionActionSpace motionActionSpace;
    public ActionSpace(CommActionSpace commActionSpace, MotionActionSpace motionActionSpace){
        this.commActionSpace = commActionSpace;
        this.motionActionSpace = motionActionSpace;
    }
}
