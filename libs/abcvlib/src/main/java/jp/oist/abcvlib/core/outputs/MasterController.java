package jp.oist.abcvlib.core.outputs;

import android.util.Log;

import java.util.ArrayList;

import ioio.lib.api.exception.ConnectionLostException;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.Switches;

public class MasterController extends AbcvlibController{

    private final String TAG = this.getClass().getName();

    private final Switches switches;
    private final ArrayList<AbcvlibController> controllers = new ArrayList<>();
    private final AbcvlibLooper abcvlibLooper;

    MasterController(Switches switches, AbcvlibLooper abcvlibLooper){
        this.switches = switches;
        this.abcvlibLooper = abcvlibLooper;
    }

    @Override
    public void run() {

        setOutput(0, 0);
        for (AbcvlibController controller : controllers){

            if (switches.loggerOn){
                Log.v(TAG, controller.toString() + "output:" + controller.getOutput().left);
            }

            setOutput((output.left + controller.getOutput().left), (output.right + controller.getOutput().right));
        }

        if (switches.loggerOn){
            Log.v("abcvlib", "grandController output:" + output.left);
        }

        abcvlibLooper.setDutyCycle(output.left, output.right);
    }

    public void addController(AbcvlibController controller){
        this.controllers.add(controller);
    }
}
