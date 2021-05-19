package jp.oist.abcvlib.core.learning;

public class CommActionSet {

    private final CommAction[] commActions;

    public CommActionSet(int actionCount){
        commActions = new CommAction[actionCount];
        if (actionCount >= 3){
            addDefaultActions();
        }
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
