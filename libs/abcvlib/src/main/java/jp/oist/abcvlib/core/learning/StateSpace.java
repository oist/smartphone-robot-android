package jp.oist.abcvlib.core.learning;

import java.util.ArrayList;

import jp.oist.abcvlib.core.inputs.AbcvlibInput;

public class StateSpace {
    public final ArrayList<AbcvlibInput> inputs;
    public StateSpace(ArrayList<AbcvlibInput> inputs){
        this.inputs = inputs;
    }
}
