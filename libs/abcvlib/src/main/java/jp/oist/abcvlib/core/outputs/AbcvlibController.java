package jp.oist.abcvlib.core.outputs;

public abstract class AbcvlibController implements Runnable{

    public Output output = new Output();

    synchronized Output getOutput(){
        return output;
    };

    protected synchronized void setOutput(float left, float right){
        output.left = left;
        output.right = right;
    };

    public class Output{
        public float left;
        public float right;
    }

}
