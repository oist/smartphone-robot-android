package jp.oist.abcvlib.serverlearning;

import org.jetbrains.annotations.NotNull;

public enum CommAction {
    // Rename these to specific actions.
    COMM_ACTION0(0, "action1"),
    COMM_ACTION1(1, "action2"),
    COMM_ACTION2(2, "action3");

    private final String actionName;
    private final int actionByte;

    CommAction(int actionNumber, String actionName) {
        this.actionName = actionName;
        this.actionByte = actionNumber;
    }

    @NotNull
    @Override
    public  String toString(){
        return actionName;
    }

    public byte getActionNumber(){
        return (byte) actionByte;
    }
}
