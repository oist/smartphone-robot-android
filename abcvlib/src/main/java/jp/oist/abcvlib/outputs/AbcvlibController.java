package jp.oist.abcvlib.outputs;

public abstract class AbcvlibController implements Runnable{

    Output output = new Output();

    Output getOutput(){
        return output;
    };

    void setOutput(double left, double right){
        output.left = left;
        output.right = right;
    };

    class Output{
        double left = 0;
        double right = 0;
    }

}
