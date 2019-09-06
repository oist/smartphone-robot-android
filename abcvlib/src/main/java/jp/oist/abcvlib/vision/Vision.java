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

    public Vision(AbcvlibMotion abcvlibMotion){
        this.abcvlibMotion = abcvlibMotion;
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

}
