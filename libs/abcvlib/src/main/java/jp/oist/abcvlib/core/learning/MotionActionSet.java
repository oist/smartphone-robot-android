package jp.oist.abcvlib.core.learning;

public class MotionActionSet {

    private MotionAction[] motionActions;

    public MotionActionSet(int actionCount){
        motionActions = new MotionAction[actionCount];
        if (actionCount >= 5){
            addDefaultActions();
        }
    }

    private void addDefaultActions(){
        motionActions[0] = new MotionAction("stop", (byte) 0, 0, 0);
        motionActions[1] = new MotionAction("forward", (byte) 1, 100, 100);
        motionActions[2] = new MotionAction("backward", (byte) 2, -100, 100);
        motionActions[3] = new MotionAction("left", (byte) 3, -100, 100);
        motionActions[4] = new MotionAction("right", (byte) 4, 100, -100);
    }

    public MotionAction[] getMotionActions() {
        return motionActions;
    }

    public void addMotionAction(String actionName, byte actionByte, int leftWheelPWM, int rightWheelPWM){
        if (actionByte > motionActions.length){
            throw new ArrayIndexOutOfBoundsException("Tried to addMotionAction to an index that " +
                    "doesn't exist in the MotionActionSet. Make sure you initialize the " +
                    "MotionActionSet with long enough length to accommodate all the actions you " +
                    "plan to create.");
        }
        motionActions[actionByte] = new MotionAction(actionName, actionByte, leftWheelPWM, rightWheelPWM);
    }
}


