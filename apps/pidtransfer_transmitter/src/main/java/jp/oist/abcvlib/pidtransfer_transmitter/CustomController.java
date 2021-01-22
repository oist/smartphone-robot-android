package jp.oist.abcvlib.pidtransfer_transmitter;

import android.util.Log;

import org.json.JSONException;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

public class CustomController extends AbcvlibController {

    private final AbcvlibActivity abcvlibActivity;

    public CustomController(AbcvlibActivity abcvlibActivity){
        this.abcvlibActivity = abcvlibActivity;
    }

    @Override
    public void run() {

        while (!abcvlibActivity.appRunning){
            Log.i("abcvlib", this.toString() + "Waiting for appRunning to be true");
            Thread.yield();
        }

        // Turn right slowly
        setOutput(-15, 15);

        }
    }

