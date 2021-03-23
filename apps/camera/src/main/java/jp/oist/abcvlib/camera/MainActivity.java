package jp.oist.abcvlib.camera;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.outputs.SocketListener;


public class MainActivity extends AbcvlibActivity implements SocketListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup a live preview of camera feed to the display. Remove if unwanted. 
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;
        initialzer(this, "192.168.28.233", 3000, null, this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onServerReadSuccess(JSONObject msgFromServer) {
        // Parse Message from Server
        // ..
        Log.i("server", msgFromServer.toString());

        // Send return message
        sendToServer();
    }

    private void sendToServer(){
        JSONObject msgToServer = new JSONObject();

        try{
            msgToServer.put("Name1", "Value1");
            msgToServer.put("Name2", "Value2");
            // Add as many JSON subobjects as you need here.
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.outputs.socketClient.writeInputsToServer(msgToServer);
    }
}
