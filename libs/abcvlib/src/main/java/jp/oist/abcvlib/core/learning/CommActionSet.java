package jp.oist.abcvlib.core.learning;

public class CommActionSet {

    private final CommAction[] commActions;

    /**
     * Initialize an object with actionCount number of discrete actions. Add actions via the
     * {@link #addCommAction(String, byte)} method. Alternatively, you can use the default actions
     * by calling this method without any arguments. See {@link #CommActionSet()}.
     * @param actionCount Number of possible discrete actions
     */
    public CommActionSet(int actionCount){
        commActions = new CommAction[actionCount];
    }

    /**
     * Creates 3 default actions called action1, action2, and action3 with bytes 0 to 2 assigned
     * respectively.
     */
    public CommActionSet(){
        commActions = new CommAction[3];
        addDefaultActions();
    }

    private void addDefaultActions(){
        commActions[0] = new CommAction("action1", (byte) 0);
        commActions[1] = new CommAction("action2", (byte) 1);
        commActions[2] = new CommAction("action3", (byte) 2);
    }

    public CommAction[] getCommActions() {
        return commActions;
    }

    public void addCommAction(String actionName, byte actionByte){
        if (actionByte > commActions.length){
            throw new ArrayIndexOutOfBoundsException();
        }
        commActions[actionByte] = new CommAction(actionName, actionByte);
    }
}
