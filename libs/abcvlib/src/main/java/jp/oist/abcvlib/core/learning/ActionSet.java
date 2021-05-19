package jp.oist.abcvlib.core.learning;

public interface ActionSet {
    MotionAction motionAction = null;
    CommAction commAction = null;

    CommAction getCommAction();
    MotionAction getMotionAction();

    void setCommAction(CommAction commAction);
    void setMotionAction(MotionAction motionAction);
}
