package jp.oist.abcvlib.serverlearning;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;

import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.FileOps;
import jp.oist.abcvlib.util.SocketListener;

public class ServerComm implements SocketListener {
    private String TAG = getClass().toString();
    private Context context;

    public ServerComm(Context context){
        this.context = context;
    }

    @Override
    public void onServerReadSuccess(JSONObject jsonHeader, ByteBuffer msgFromServer) {
        // Parse whatever you sent from python here
        //loadMappedFile...
        try {
            if (jsonHeader.get("content-encoding").equals("modelVector")){
                Log.d(TAG, "Writing model files to disk");
                JSONArray modelNames = (JSONArray) jsonHeader.get("model-names");
                JSONArray modelLengths = (JSONArray) jsonHeader.get("model-lengths");

                msgFromServer.flip();

                for (int i = 0; i < modelNames.length(); i++){
                    byte[] bytes = new byte[modelLengths.getInt(i)];
                    msgFromServer.get(bytes);
                    FileOps.savedata(context, bytes, "models", modelNames.getString(i) + ".tflite");
                }
            }else{
                Log.d(TAG, "Data from server does not contain modelVector content. Be sure to set content-encoding to \"modelVector\" in the python jsonHeader");
            }
        } catch (JSONException e) {
            ErrorHandler.eLog(TAG, "Something wrong with parsing the JSONheader from python", e, true);
        }
    }
}
