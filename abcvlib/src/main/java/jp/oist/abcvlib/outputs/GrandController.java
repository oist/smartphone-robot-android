package jp.oist.abcvlib.outputs;

import android.util.Log;

import java.util.ArrayList;

import jp.oist.abcvlib.AbcvlibActivity;

public class GrandController extends AbcvlibController{

    private AbcvlibActivity abcvlibActivity;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();

    GrandController(AbcvlibActivity abcvlibActivity, ArrayList<AbcvlibController> controllers){
        this.abcvlibActivity = abcvlibActivity;
        this.controllers = controllers;
    }

    @Override
    public Output getOutput() {
        return output;
    }

    public void setOutput(double left, double right) {
        output.left = left;
        output.right = right;
    }

    @Override
    public void run() {

        while (abcvlibActivity.appRunning){
            setOutput(0, 0);
            for (AbcvlibController controller : controllers){
                setOutput((this.output.left + controller.output.left), (this.output.left + controller.output.right));
            }
            Log.d("abcvlib", "grandController output:" + output.left);
            abcvlibActivity.outputs.motion.setWheelOutput((int) output.left, (int) output.right);
        }

    }

    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }

}
