package jp.oist.abcvlib.serverlearning;

import org.jetbrains.annotations.NotNull;

public enum CommAction {
    // Rename these to specific actions.
    COMM_ACTION1("action1", 0),
    COMM_ACTION2("action2", 1),
    COMM_ACTION3("action3", 2);

    private final String actionName;
    private final int actionNumber;


    CommAction(String actionName, int actionNumber) {
        this.actionName = actionName;
        this.actionNumber = actionNumber;
    }

    @NotNull
    @Override
    public  String toString(){
        return actionName;
    }

    public int getActionNumber(){
        return actionNumber;
    }
}
