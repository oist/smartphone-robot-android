package jp.oist.abcvlib.inputs.vision;

import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.oist.abcvlib.AbcvlibActivity;
import jp.oist.abcvlib.R;

import static android.Manifest.permission.CAMERA;

public class Vision implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private AbcvlibActivity abcvlibActivity;
    private double phi = 0;
    private double CENTER_COL;
    private double CENTER_ROW;
    private double height;
    private double width;

    public static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private int mCameraId = 0;
    public CameraBridgeViewBase mOpenCvCameraView;

    protected static final String TAG = "abcvlib.vision";

    private int frameHeight;
    private int frameWidth;

    protected Scalar               mBlobColorRgba;
    protected Scalar               mBlobColorHsv;
    protected ColorBlobDetector mDetector;
    protected Mat                  mSpectrum;
    protected Size                 SPECTRUM_SIZE;
    protected Scalar               CONTOUR_COLOR;
    public Mat                  mRgba;
    public Mat                  mRgbaTrans;
    protected Double               CENTER_THRESHOLD = 0.1; // How far centroid can be from absolute center before being considered centered.
    protected boolean              mIsColorSelected = false;
    protected List<MatOfPoint>     contours;
    protected double[]             blobSizes;
    private List<Point> centroids;


    // Todo this needs to be created before onCreate is called. How to ensure this?
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(abcvlibActivity) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMaxFrameSize(400,400);
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(Vision.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Vision(final AbcvlibActivity abcvlibActivity, int height, int width){
        this.abcvlibActivity = abcvlibActivity;

        if (mOpenCvCameraView == null & abcvlibActivity.switches.cameraApp){
            abcvlibActivity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            abcvlibActivity.setContentView(R.layout.color_blob_detection_surface_view);
            mOpenCvCameraView = abcvlibActivity.findViewById(R.id.color_blob_detection_activity_surface_view);
            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.enableFpsMeter();
            // TODO this needs to be set only after setMaxFrameSize from CameraBridgeViewBase is set.
            this.CENTER_COL = mOpenCvCameraView.frameHeight / 2.0;
            this.CENTER_ROW = mOpenCvCameraView.frameWidth / 2.0;
            // I'd think there is a better way to fix the orientation in portrait without cropping everyting
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            swapCamera();
//            OpenCVLoader.initDebug();
        }

    }

    public void onStart(){
        boolean havePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (abcvlibActivity.checkSelfPermission(CAMERA) != PackageManager.PERMISSION_GRANTED) {
                abcvlibActivity.requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                havePermission = false;
            }
        }
        if (havePermission) {
            onCameraPermissionGranted();
        }
    }

    public void onPause(){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onResume(){
        if (abcvlibActivity.switches.cameraApp){
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, abcvlibActivity, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    public void onDestroy(){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private void swapCamera() {
        mCameraId = mCameraId^1; //bitwise not operation to flip 1 to 0 and vice versa
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    public void onCameraPermissionGranted() {
        List<? extends CameraBridgeViewBase> cameraViews = getCameraViewList();
        if (cameraViews == null) {
            return;
        }
        for (CameraBridgeViewBase cameraBridgeViewBase: cameraViews) {
            if (cameraBridgeViewBase != null) {
                cameraBridgeViewBase.setCameraPermissionGranted();
            }
        }
    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
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

    public void onCameraViewStarted(int width, int height) {

        this.frameHeight = height;
        this.frameWidth = width;
        // TODO check if width and height are transposed or not. Check if set via setMaxFrameSize or not.
        CENTER_COL = height / 2.0;
        CENTER_ROW = width / 2.0;

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaTrans = new Mat(width, height, CvType.CV_8UC4);

        Log.i(TAG, "CENTER_COL:" + CENTER_COL + " CENTER_ROW:" + CENTER_ROW);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(100, 32);
        CONTOUR_COLOR = new Scalar(255,0,0,255);

        // Hardcoded to red of puck
//        Scalar mBlobColorHsv = new Scalar(7.4, 235.7, 222.6, 0.0);
        Scalar mBlobColorHsv = new Scalar(0.5, 251, 166, 0.0);
        mDetector.setHsvColor(mBlobColorHsv);
        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);
        mIsColorSelected = true;
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int width = mOpenCvCameraView.getWidth();
        int height = mOpenCvCameraView.getHeight();

        int xOffset = (width - cols) / 2;
        int yOffset = (height - rows) / 2;

        Log.i(TAG, "width:" + width + ", height:" + height);
        Log.i(TAG, "cols:" + cols + ", rows:" + rows);
        Log.i(TAG, "x:" + String.valueOf(event.getX()) + ", y:" + String.valueOf((event.getY())));

//        int x = (int)event.getX() - xOffset;
//        int y = (int)event.getY() - yOffset;
//        int x = Math.round(event.getX() * ((float)rows / (float)mOpenCvCameraView.getWidth()));
//        int y =  Math.round(event.getY() * ((float)cols / (float)mOpenCvCameraView.getHeight()));
        int y = Math.round((width - event.getX()) * ((float)rows / (float)mOpenCvCameraView.getWidth()));
        int x =  Math.round((height - event.getY()) * ((float)cols / (float)mOpenCvCameraView.getHeight()));


        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) {
            Log.i(TAG, "Row or Col count wrong");
            return false;
        }

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        Log.i("colorpicker", "Touched HSV color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

//        Scalar mBlobColorHsv = new Scalar(0.0, 100, 100, 0.0);

        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE, 0, 0, Imgproc.INTER_LINEAR_EXACT);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Core.transpose(mRgba, mRgbaTrans);

        if (mIsColorSelected){
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.v(TAG, "Contours count: " + contours.size());

            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

            centroids = Centroids(contours);

            blobSizes = new double[contours.size()];
            for (int i = 0; i < contours.size(); i++) {
                blobSizes[i] = Imgproc.contourArea(contours.get(i));
            }

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }
        return mRgba;
    }

    public List<Point> getCentroids(){
        return centroids;
    }

    public double[] getBlobSizes(){
        return blobSizes;
    }

    public double getCENTER_COL(){
        return CENTER_COL;
    }

}
