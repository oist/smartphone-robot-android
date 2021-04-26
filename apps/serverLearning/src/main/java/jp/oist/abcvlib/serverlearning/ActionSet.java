package jp.oist.abcvlib.serverlearning;

public class ActionSet {
    private MotionAction motionAction;
    private CommAction commAction;

    public ActionSet(MotionAction motionAction, CommAction commAction){
        this.motionAction = motionAction;
        this.commAction = commAction;
    }

    public CommAction getCommAction() {
        return commAction;
    }

    public MotionAction getMotionAction() {
        return motionAction;
    }

    public void setCommAction(CommAction commAction) {
        this.commAction = commAction;
    }

    public void setMotionAction(MotionAction motionAction) {
        this.motionAction = motionAction;
    }
}
