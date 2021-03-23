package jp.oist.abcvlib.serverlearning;

import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class Analyzer implements ImageAnalysis.Analyzer {

    public Analyzer(){

    }

    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();
            byte[] frame = new byte[width * height];
            Image.Plane[] planes = image.getPlanes();
            int idx = 0;
            for (Image.Plane plane : planes){
                ByteBuffer frameBuffer = plane.getBuffer();
                int n = frameBuffer.limit();
                Log.i("analyzer", "Plane: " + idx + " width: " + width + " height: " + height + " WxH: " + width*height + " limit: " + n);
//                        frameBuffer.flip();
                frame = new byte[n];
                frameBuffer.get(frame);
                frameBuffer.clear();
                idx++;
            }
        }
        imageProxy.close();
    }
}
