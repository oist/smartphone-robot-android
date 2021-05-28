package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.ArrayList;

import ioio.lib.api.exception.ConnectionLostException;
import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;
import jp.oist.abcvlib.util.ErrorHandler;

public class GrandController extends AbcvlibController{

    private final String TAG = this.getClass().getName();

    private Switches switches;
    private ArrayList<AbcvlibController> controllers;
    private AbcvlibLooper abcvlibLooper;

    GrandController(Switches switches, AbcvlibLooper abcvlibLooper){
        this.switches = switches;
        this.abcvlibLooper = abcvlibLooper;
    }

    @Override
    public void run() {

        if (controllers != null){
            setOutput(0, 0);
            for (AbcvlibController controller : controllers){

                Output controllerOutput = controller.getOutput();

                if (switches.loggerOn){
                    Log.v("grandcontroller", controller.toString() + "output:" + controllerOutput.left);
                }

                setOutput((output.left + controllerOutput.left), (output.right + controllerOutput.right));
            }

            if (switches.loggerOn){
                Log.v("abcvlib", "grandController output:" + output.left);
            }

            try {
                abcvlibLooper.setDutyCycle((int) output.left, (int) output.right);
            } catch (ConnectionLostException e) {
                e.printStackTrace();
            }
        }
    }


    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }

}
