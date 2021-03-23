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
import jp.oist.abcvlib.core.inputs.vision.ImageAnalyzerActivity;
import jp.oist.abcvlib.core.outputs.SocketListener;


public class MainActivity extends AbcvlibActivity implements SocketListener, ImageAnalyzerActivity {

    public ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup a live preview of camera feed to the display. Remove if unwanted. 
        setContentView(jp.oist.abcvlib.core.R.layout.camera_x_preview);

        switches.pythonControlledPIDBalancer = true;
        switches.cameraXApp = true;
        initialzer(this, "192.168.28.233", 3000, null, this, this);
        super.onCreate(savedInstanceState);

        /*
         * Setup CameraX ImageAnalysis Use Case
         * ref: https://developer.android.com/reference/androidx/camera/core/ImageAnalysis.Builder
          */
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

        // Link the Analysis to the CustomImageAnalyzer class (same dir as this class)
        imageAnalysis.setAnalyzer(inputs.camerax.analysisExecutor, new CustomImageAnalyzer());
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

    // Passes custom ImageAnalysis object to core CameraX lib to bind to lifecycle, and other admin functions
    @Override
    public ImageAnalysis getAnalyzer() {
        return imageAnalysis;
    }
}
