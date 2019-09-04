package jp.oist.abcvlib.vision;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class Vision {
    public List<Point> Centroids(List<MatOfPoint> contour) {
        List<Point> centroids = new ArrayList<Point>();
        for (int i = 0; i < contour.size(); i++) {

            Moments moments = Imgproc.moments(contour.get(i), true);

            // Not sure if this works or not. Could have nullpointer exception if length of arraylist not set before?
            centroids.get(i).x = moments.get_m10() / moments.get_m00();
            centroids.get(i).y = moments.get_m01() / moments.get_m00();

        }
        return centroids;
    }

}
