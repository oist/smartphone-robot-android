package jp.oist.abcvlib.core.learning;

public class CommAction {
    String actionName;
    byte actionByte;

    public CommAction(String actionName, byte actionByte){
        this.actionName = actionName;
        this.actionByte = actionByte;
    }

    public String getActionName(){return actionName;};
    public int getActionByte(){return  actionByte;};

    public void setActionName(String actionName){this.actionName = actionName;};
    public void setActionByte(byte actionByte){this.actionByte = actionByte;};
}
