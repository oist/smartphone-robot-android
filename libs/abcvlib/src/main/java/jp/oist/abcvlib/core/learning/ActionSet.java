package jp.oist.abcvlib.core.learning;

public class ActionSet {
    MotionAction motionAction;;
    CommAction commAction;

    public ActionSet(MotionAction motionAction, CommAction commAction){
        this.motionAction = motionAction;
        this.commAction = commAction;
    }

    public CommAction getCommAction(){return commAction;}
    public MotionAction getMotionAction(){return motionAction;}

    public void setCommAction(CommAction commAction){this.commAction = commAction;}
    public void setMotionAction(MotionAction motionAction){this.motionAction = motionAction;}
}
