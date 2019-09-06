package jp.oist.abcvlib.colorblobdetect;

import java.util.List;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.android.CameraBridgeViewBase;

import android.os.Bundle;
import android.view.Window;

import jp.oist.abcvlib.basic.AbcvlibActivity;

public class ColorBlobDetectionActivity extends AbcvlibActivity {


    private List<Point>          centroids;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.color_blob_detection_surface_view);
        super.mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);

        super.onCreate(savedInstanceState);

    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (mIsColorSelected) {
            mDetector.process(mRgba);
            contours = mDetector.getContours();
            super.onCameraFrame(inputFrame);
            centroids = vision.Centroids(contours);
            vision.centerBlob(centroids, CENTER_ROW, CENTER_THRESHOLD);
        }

        return mRgba;
    }



}
