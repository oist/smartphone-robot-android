package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.ArrayList;

import ioio.lib.api.exception.ConnectionLostException;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;

public class GrandController extends AbcvlibController{

    private final String TAG = this.getClass().getName();

    private final Switches switches;
    private final ArrayList<AbcvlibController> controllers = new ArrayList<>();
    private final AbcvlibLooper abcvlibLooper;

    GrandController(Switches switches, AbcvlibLooper abcvlibLooper){
        this.switches = switches;
        this.abcvlibLooper = abcvlibLooper;
    }

    @Override
    public void run() {

        setOutput(0, 0);
        for (AbcvlibController controller : controllers){

            if (switches.loggerOn){
                Log.v("grandcontroller", controller.toString() + "output:" + controller.getOutput().left);
            }

            setOutput((output.left + controller.getOutput().left), (output.right + controller.getOutput().right));
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

    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }
}
