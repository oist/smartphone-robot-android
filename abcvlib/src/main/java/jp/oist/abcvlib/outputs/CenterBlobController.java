package jp.oist.abcvlib.outputs;

import android.util.Log;

import org.json.JSONException;
import org.opencv.core.Point;

import java.util.Arrays;
import java.util.List;

import jp.oist.abcvlib.AbcvlibActivity;

public class CenterBlobController implements Runnable{


    private AbcvlibActivity abcvlibActivity;

    private int avgCount = 1000;
    private double phi;
    private double CENTER_COL;

    public CenterBlobController(AbcvlibActivity abcvlibActivity){

        this.abcvlibActivity = abcvlibActivity;
        this.CENTER_COL = abcvlibActivity.inputs.vision.getCENTER_COL();

    }

    public void run(){

        while(abcvlibActivity.appRunning && abcvlibActivity.centerBlobApp) {

            phi = getPhi(abcvlibActivity.inputs.vision.getCentroid());

        }
    }

    /**
     * Phi is not an actual measure of degrees or radians, but relative to the pixel density from the camera
     * For example, if the centroid of interest is at the center of the vertical plane, phi = 0.
     * If the centroid of interest if at the leftmost part of the screen, phi = -1. Likewise, if
     * at the rightmost part of the screen, then phi = 1. As the actual angle depends on the optics
     * of the camera, this is just a first attempt, but OpenCV may have more robust/accuarte 3D motionSensors
     * metrics.
     * @return
     */
    public double getPhi(List<Point> centroid){

        //TODO handle multiple centroids somehow.
        //TODO make centroid object thread-safe somehow. (Executor, ThreadPoolExecutor, and onPostExecute)
        try {
            phi = (CENTER_COL - centroid.get(0).y) / CENTER_COL;
        } catch (IndexOutOfBoundsException e){
            // This will happen fairly regularly I suppose since both PID thread and onCameraFrame
            // both use the same object. If onCameraFrame fires after
            // ColorBlobDetectionActivity.linearController finishes, then the initial value of centroid
            // could change to a null value before the above code executes.
            Log.e("abcvlib", "Index out of bounds exception on centroid object.");
        }

        return phi;
    }

}
