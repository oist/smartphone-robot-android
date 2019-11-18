package jp.oist.abcvlib.outputs;

import org.json.JSONObject;

import java.util.HashMap;

import jp.oist.abcvlib.AbcvlibActivity;

public class Outputs implements OutputsInterface {

    private Thread centerBlobControllerThread;
    private CenterBlobController centerBlobController;
    protected Thread pidControllerThread;
    public Motion motion;
    protected SocketClient socketClient;
    private Thread socketClientThread;
    public BalancePIDController balancePIDController;

    public Outputs(AbcvlibActivity abcvlibActivity, String hostIP, int port){

        //BalancePIDController Controller
        motion = new Motion(abcvlibActivity);

        // Python Socket Connection. Host IP:Port needs to be the same as python server.
        // Todo: automatically detect host server or set this to static IP:Port. Tried UDP Broadcast,
        //  but seems to be blocked by router. Could set up DNS and static hostname, but would
        //  require intervention with IT
        if (abcvlibActivity.pythonControlApp){
            socketClient = new SocketClient(hostIP, port, abcvlibActivity.inputs.stateVariables,
                    controls, abcvlibActivity);
            socketClientThread = new Thread(socketClient);
            socketClientThread.start();
        }

        if (abcvlibActivity.balanceApp){
            balancePIDController = new BalancePIDController(abcvlibActivity);
            pidControllerThread = new Thread(balancePIDController);
            pidControllerThread.start();
        }

        // Todo need some method to handle combining balancePIDController output with another controller
        //  (centerBlobApp, setPath, etc.). Maybe some grandController on another thread that reads in the
        //  output from balancePIDController along with the output from e.g. centerBlobApp() and path().
        if (abcvlibActivity.centerBlobApp){
            centerBlobController = new CenterBlobController(abcvlibActivity);
            centerBlobControllerThread = new Thread(centerBlobController);
            centerBlobControllerThread.start();
        }
//
//        grandController = new GrandController(abcvlibActivity, loggerOn, balancePIDController, centerBlobController);
//        grandControllerThread = new Thread(grandController);
//        grandControllerThread.start();
    }

    private void grandController(){

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
