package jp.oist.abcvlib.core.learning;

public class MotionActionSpace {

    private MotionAction[] motionActions;

    /**
     * Initialize an object with actionCount number of discrete actions. Add actions via the
     * {@link #addMotionAction(String, byte, float, float, boolean, boolean)} method. Alternatively, you can use the default actions
     * by calling this method without any arguments. See {@link #MotionActionSpace()}.
     * @param actionCount Number of possible discrete actions
     */
    public MotionActionSpace(int actionCount){
        motionActions = new MotionAction[actionCount];
    }

    /**
     * Creates default motion action set with stop, forward, backward, left, and right as actions.
     */
    public MotionActionSpace(){
        motionActions = new MotionAction[5];
        addDefaultActions();
    }

    private void addDefaultActions(){
        motionActions[0] = new MotionAction("stop", (byte) 0, 0, 0, true, true);
        motionActions[1] = new MotionAction("forward", (byte) 1, 1, 1, false, false);
        motionActions[2] = new MotionAction("backward", (byte) 2, -1, -1, false, false);
        motionActions[3] = new MotionAction("left", (byte) 3, -1, 1, false, false);
        motionActions[4] = new MotionAction("right", (byte) 4, 1, -1, false, false);
    }

    public MotionAction[] getMotionActions() {
        return motionActions;
    }

    public void addMotionAction(String actionName, byte actionByte, float leftWheelSpeed,
                                float rightWheelSpeed, boolean leftWheelBrake, boolean rightWheelBrake){
        if (actionByte > motionActions.length){
            throw new ArrayIndexOutOfBoundsException("Tried to addMotionAction to an index that " +
                    "doesn't exist in the MotionActionSpace. Make sure you initialize the " +
                    "MotionActionSpace with long enough length to accommodate all the actions you " +
                    "plan to create.");
        }
        motionActions[actionByte] = new MotionAction(actionName, actionByte, leftWheelSpeed,
                rightWheelSpeed, leftWheelBrake, rightWheelBrake);
    }
}


