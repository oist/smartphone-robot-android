package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.ArrayList;

import jp.oist.abcvlib.core.AbcvlibActivity;

public class GrandController extends AbcvlibController{

    private final String TAG = this.getClass().getName();

    private AbcvlibActivity abcvlibActivity;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();

    GrandController(AbcvlibActivity abcvlibActivity, ArrayList<AbcvlibController> controllers){
        this.abcvlibActivity = abcvlibActivity;
        this.controllers = controllers;
    }

    @Override
    public void run() {

        while (!abcvlibActivity.appRunning){
            try {
                Log.i("abcvlib", this.toString() + "Waiting for appRunning to be true");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG,"Error", e);
            }
        }

        setOutput(0, 0);
        for (AbcvlibController controller : controllers){

            Output controllerOutput = controller.getOutput();

                if (abcvlibActivity.switches.loggerOn){
                    Log.v("grandcontroller", controller.toString() + "output:" + controllerOutput.left);
                }

                setOutput((output.left + controllerOutput.left), (output.right + controllerOutput.right));
            }

        if (abcvlibActivity.switches.loggerOn){
            Log.v("abcvlib", "grandController output:" + output.left);
        }
        abcvlibActivity.outputs.motion.setWheelOutput((int) output.left, (int) output.right);
    }


    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }

}
