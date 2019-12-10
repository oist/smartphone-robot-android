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
    public void run() {

        while (abcvlibActivity.appRunning){
            setOutput(0, 0);
            for (AbcvlibController controller : controllers){

                Output controllerOutput = controller.getOutput();

                Log.d("abcvlib", controller.toString() + "output:" + controllerOutput.left);

                setOutput((output.left + controllerOutput.left), (output.right + controllerOutput.right));
            }
            Log.d("abcvlib", "grandController output:" + output.left);
            abcvlibActivity.outputs.motion.setWheelOutput((int) output.left, (int) output.right);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }

}
