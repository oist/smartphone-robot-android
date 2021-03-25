package jp.oist.abcvlib.serverlearning;

import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.WithHint;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibApp;
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;
import jp.oist.abcvlib.core.outputs.SocketListener;


public class MainActivity extends AbcvlibActivity implements SocketListener, AbcvlibApp {

    private DataGatherer dataGatherer;
    private MsgToServer msgToServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup a live preview of camera feed to the display. Remove if unwanted. 
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;

        msgToServer = new MsgToServer();
        dataGatherer = new DataGatherer(this, msgToServer);

        initialzer(this, "192.168.28.233", 3000, null, this, dataGatherer);
        super.onCreate(savedInstanceState);

        dataGatherer.start();
    }

    @Override
    public void onServerReadSuccess(JSONObject msgFromServer) {
        // Parse Message from Server
        // ..
        Log.i("server", msgFromServer.toString());

        // Send return message
        sendToServer();
    }

    /**
     * Assemble message to server and send.
     */
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

    @Override
    public void initFinished() {

    }
}
