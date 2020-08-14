package jp.oist.abcvlib.core.outputs;

public abstract class AbcvlibController implements Runnable{

    public Output output = new Output();

    Output getOutput(){
        return output;
    };

    protected void setOutput(double left, double right){
        output.left = left;
        output.right = right;
    };

    public class Output{
        public double left;
        public double right;
    }

}
