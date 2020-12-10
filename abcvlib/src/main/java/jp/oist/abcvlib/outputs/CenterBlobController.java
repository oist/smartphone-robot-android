package jp.oist.abcvlib.outputs;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.opencv.core.Point;

import java.util.List;

import jp.oist.abcvlib.AbcvlibActivity;

public class CenterBlobController extends AbcvlibController{


    private AbcvlibActivity abcvlibActivity;

    private double phi = 0;
    private double CENTER_COL;
    private double p_phi = -35;
    private List<Point> centroids;
    int noBlobInFrameCounter = 0;
    int blobInFrameCounter = 0;
    int backingUpFrameCounter = 0;
    long noBlobInFrameStartTime;
    long backingUpStartTime;
    long searchingFrameCounter;
    long searchingStartTime;

    public CenterBlobController(AbcvlibActivity abcvlibActivity){

        this.abcvlibActivity = abcvlibActivity;

    }

    public void run(){

        while (!abcvlibActivity.appRunning){
            try {
                Log.i("abcvlib", this.toString() + "Waiting for appRunning to be true");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while(abcvlibActivity.appRunning && abcvlibActivity.switches.centerBlobApp) {

//            Log.d("abcvlib", "in CenterBlobController 1");

            centroids = abcvlibActivity.inputs.vision.getCentroids();
            double[] blobSizes = abcvlibActivity.inputs.vision.getBlobSizes();
            double staticApproachSpeed = 50;
            double variableApproachSpeed = 0;

            if (abcvlibActivity.outputs.socketClient.socketMsgIn != null){
                try {
                    p_phi = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("p_phi").toString());
                    Log.d("centerblob", "p_phi:" + p_phi);
                    variableApproachSpeed = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("wheelSpeedL").toString());
                    Log.d("centerblob", "wheelSpeed:" + variableApproachSpeed);
                    staticApproachSpeed = Double.parseDouble(abcvlibActivity.outputs.socketClient.socketMsgIn.get("staticApproachSpeed").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Todo eliminate hard dependence on python connection. Should be able to run alone with hard set values;
            // If there are blobs with centroids...
            if (centroids != null && blobSizes != null && blobSizes.length != 0){

                noBlobInFrameCounter = 0;
                backingUpFrameCounter = 0;
                blobInFrameCounter++;

                phi = getPhi(centroids);
                Log.d("centerblob", "phi:" + phi);
                Log.d("centerblob", "Blob size:" + blobSizes[0]);

                // Todo check polarity on these turns. Could be opposite
                double outputLeft = -(phi * p_phi) + (staticApproachSpeed + (variableApproachSpeed / blobSizes[0]));
                double outputRight = (phi * p_phi) + (staticApproachSpeed + (variableApproachSpeed / blobSizes[0]));
                setOutput(outputLeft, outputRight);

                Log.d("centerblob", "CenterBlobController left:" + output.left + " right:" + output.right);

            }
            else{

                Log.v("centerblob", "No blobs in sight");

                if (noBlobInFrameCounter == 0) {
                    noBlobInFrameStartTime = System.nanoTime();
                }

                noBlobInFrameCounter++;
                blobInFrameCounter = 0;

                Log.v("centerblob", "No blobs. Prior to timing logic");

                // If no blob in frame for 3.5 seconds...
                double approachTime = 3.5e9;
                // How long to backup after landing on puck in nanoseconds.
                double backupTime = 2e9;
                // How long to search after backing up in nanoseconds.
                double searchTime = 5e9;
                if (System.nanoTime() - noBlobInFrameStartTime > (approachTime)){
                    // If just starting to backup
                    if (backingUpFrameCounter == 0){
                        backingUpStartTime = System.nanoTime();
                        Log.v("centerblob", "No blobs. Setting backingupStartTime = 0");
                        backingUpFrameCounter++;
                    }
                    // If backing up for more than 3 seconds.
                    else if (System.nanoTime() - backingUpStartTime > backupTime) {
                        if (searchingFrameCounter == 0) {
                            searchingStartTime = System.nanoTime();
                            searchingFrameCounter++;
                        }
                        // Searching for less than 3 sec? Continue to search (turn).
                        else if (System.nanoTime() - searchingStartTime < searchTime) {
                            setOutput(35, 0);
                            searchingFrameCounter++;
                        }
                        // Searching for more than 3 sec? Try backing up again.
                        else{
                            backingUpFrameCounter = 0;
                            searchingFrameCounter = 0;
                        }
                    }
                    // Continue backing up
                    else{
                        double outputLeft = -2.0 * staticApproachSpeed;
                        double outputRight = outputLeft;
                        setOutput(outputLeft, outputRight);
                        backingUpFrameCounter++;
                    }
                }
                // blob has not been in frame for less than 3 seconds. Continue Forward.
                else{
                    double outputLeft = staticApproachSpeed;
                    double outputRight = staticApproachSpeed;
                    setOutput(outputLeft, outputRight);
                }
            }
            Thread.yield();
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
            this.CENTER_COL = abcvlibActivity.inputs.vision.getCENTER_COL();
            phi = (CENTER_COL - centroid.get(0).y) / CENTER_COL;
        } catch (IndexOutOfBoundsException e){
            // This will happen fairly regularly I suppose since both PID thread and onCameraFrame
            // both use the same object. If onCameraFrame fires after
            // ColorBlobDetectionActivity.linearController finishes, then the initial value of centroid
            // could change to a null value before the above code executes.
            Log.e("abcvlib", "Index out of bounds exception on centroid object.");
        } catch (NullPointerException e){
            Log.e("abcvlib", "centroid not availble for find phi yet");
            e.printStackTrace();
        }

        return phi;
    }

}
