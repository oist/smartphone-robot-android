package jp.oist.abcvlib.vision;

import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

import jp.oist.abcvlib.basic.AbcvlibMotion;

public class Vision {

    private AbcvlibMotion abcvlibMotion;
    private double phi = 0;
    private double CENTER_COL;
    private double CENTER_ROW;
    private double height;
    private double width;

    public Vision(AbcvlibMotion abcvlibMotion, int height, int width){
        this.abcvlibMotion = abcvlibMotion;
        this.height = height;
        this.width = width;
        // TODO check is the col and rows are not transposed.
        this.CENTER_COL = height / 2.0;
        this.CENTER_ROW = width / 2.0;
    }

    public List<Point> Centroids(List<MatOfPoint> contour) {
        List<Point> centroids = new ArrayList<Point>(contour.size());
        for (int i = 0; i < contour.size(); i++){
            centroids.add(new Point());
        }
        for (int i = 0; i < contour.size(); i++) {

            Moments moments = Imgproc.moments(contour.get(i), true);

            // Not sure if this works or not. Could have nullpointer exception if length of arraylist not set before?
            centroids.get(i).x = moments.get_m10() / moments.get_m00();
            centroids.get(i).y = moments.get_m01() / moments.get_m00();

        }
        return centroids;
    }

    /**
     * Take centroids, determine which direction to turn then send wheel speed
     */
    public void centerBlob(List<Point> centroids, Double CENTER_ROW, Double CENTER_THRESHOLD){

        if (centroids.size() > 0){
            // For whatever reason, the rows below physically corresponds to the columns of the screen
            // when in portrait mode. y=0 at top right of screen, increases toward left
            // x=0 at top right of screen, increases as moving down.
            if (centroids.get(0).y > (CENTER_ROW + (CENTER_ROW * CENTER_THRESHOLD))){
                // Turn right
                abcvlibMotion.setWheelSpeed(300,0);
                Log.i("abcvlib", "turning right");
            } else if (centroids.get(0).y < (CENTER_ROW - (CENTER_ROW * CENTER_THRESHOLD))){
                // Turn left
                abcvlibMotion.setWheelSpeed(0,300);
                Log.i("abcvlib", "turning left");
            } else {
                // Stay put
                abcvlibMotion.setWheelSpeed(0,0);
                Log.i("abcvlib", "Blob is within threshold. Staying put");
            }
            Log.i("abcvlib", "centroid y @" + centroids.get(0).y);
        }
    }

    /**
     * Phi is not an actual measure of degrees or radians, but relative to the pixel density from the camera
     * For example, if the centroid of interest is at the center of the vertical plane, phi = 0.
     * If the centroid of interest if at the leftmost part of the screen, phi = -1. Likewise, if
     * at the rightmost part of the screen, then phi = 1. As the actual angle depends on the optics
     * of the camera, this is just a first attempt, but OpenCV may have more robust/accuarte 3D spatial
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
