package jp.oist.abcvlib.outputs;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.oist.abcvlib.AbcvlibActivity;

public class Outputs implements OutputsInterface {

    private Thread centerBlobControllerThread;
    private CenterBlobController centerBlobController;
    protected Thread pidControllerThread;
    public Motion motion;
    public SocketClient socketClient;
    private Thread socketClientThread;
    public BalancePIDController balancePIDController;
    private GrandController grandController;
    private Thread grandControllerThread;
    private ArrayList<AbcvlibController> controllers = new ArrayList<>();

    public Outputs(AbcvlibActivity abcvlibActivity, String hostIP, int port, AbcvlibController controller){

        if (controller != null){
            controllers.add(controller);
        }

        //BalancePIDController Controller
        motion = new Motion(abcvlibActivity);

        // Todo: make all these threads self contained like in MicrophoneInput so you can set join/stop methods
        // Python Socket Connection. Host IP:Port needs to be the same as python server.
        // Todo: automatically detect host server or set this to static IP:Port. Tried UDP Broadcast,
        //  but seems to be blocked by router. Could set up DNS and static hostname, but would
        //  require intervention with IT
        if (abcvlibActivity.switches.pythonControlApp){
            socketClient = new SocketClient(hostIP, port, abcvlibActivity.inputs.stateVariables,
                    controls, abcvlibActivity);
            socketClientThread = new Thread(socketClient);
            socketClientThread.start();
            Log.v("abcvlib", "socketClient Started");
        }

        if (abcvlibActivity.switches.balanceApp){
            balancePIDController = new BalancePIDController(abcvlibActivity);
            pidControllerThread = new Thread(balancePIDController);
            pidControllerThread.start();
            controllers.add(balancePIDController);
            Log.v("abcvlib", "BalanceApp Started");

        }

        // Todo need some method to handle combining balancePIDController output with another controller
        //  (centerBlobApp, setPath, etc.). Maybe some grandController on another thread that reads in the
        //  output from balancePIDController along with the output from e.g. centerBlobApp() and path().
        if (abcvlibActivity.switches.centerBlobApp){
            centerBlobController = new CenterBlobController(abcvlibActivity);
            centerBlobControllerThread = new Thread(centerBlobController);
            centerBlobControllerThread.start();
            controllers.add(centerBlobController);
        }
//
        if (!controllers.isEmpty()){
            grandController = new GrandController(abcvlibActivity, controllers);
            grandControllerThread = new Thread(grandController);
            grandControllerThread.start();
        }
    }

    @Override
    public void setControls(JSONObject controls) {

    }

    @Override
    public void setAudioFile() {

    }

    @Override
    public void setWheelOutput(int left, int right) {

    }

    @Override
    public void setPID() {

    }

}
