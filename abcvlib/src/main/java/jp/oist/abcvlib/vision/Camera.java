package jp.oist.abcvlib.vision;

import android.view.MotionEvent;
import android.view.View;

import jp.oist.abcvlib.basic.AbcvlibSensors;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by iffor_000 on 12/14/2016.
 */
public class Camera implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener{

    private boolean mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private BlobDetector mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;

    double ncx, ncy, ncxn;
    double cx, cy;
    public static double distance=10;
    public static double area;
    public static double contourdiff;

    private AbcvlibSensors abcvlibSensors;

    public Camera(AbcvlibSensors abcvlibSensors){
        this.abcvlibSensors = abcvlibSensors;
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new BlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        double thetaDeg = abcvlibSensors.getThetaDeg();


        Core.flip(mRgba, mRgba, 1);
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            //Log.e(TAG, "Contours count: " + contours.size());


            Scalar green = new Scalar(0, 255, 0);
            Scalar red=new Scalar(255,0,0);
            Scalar blue=new Scalar(0,0,255);

            Imgproc.drawContours(mRgba, contours, -1, red, 10);

            Core.circle(mRgba, mDetector.Centroid(contours), 10, green, 10);
            Core.circle(mRgba, mDetector.getleftleast(contours),10,blue,10);
            Core.circle(mRgba, mDetector.getrightleast(contours),10,blue,10);

            area=mDetector.getContourarea();

            if(area>5000){

                cx=mDetector.Centroid(contours).y;
                cy=mDetector.Centroid(contours).x;
                //ncx=(1.0-cx/mOpenCvCameraView.getHeight()-0.7);
                ncx=(1.0-cx/480-0.5)*0.6; //480=rgba.width
                //ncy=1.0-cy/mOpenCvCameraView.getWidth();
                ncy=1.0-cy/800; //800=rgba.height


                double llx=mDetector.getleftleast(contours).x;
                double lly=mDetector.getleftleast(contours).y;
                double rlx=mDetector.getrightleast(contours).x;
                double rly=mDetector.getrightleast(contours).y;
                contourdiff=(llx-rlx)/350;

                Scalar fontColor = new Scalar(0, 128, 128);
                distance=(18.9842+ thetaDeg *2.7743+ncy*276.1399+2.7267* thetaDeg *ncy)/10-2.6;


            }else {

                if(contourdiff<0){
                    contourdiff=-0.4;
                }else{
                    contourdiff=0.4;
                }
                distance=15;

            }

        }else{

        }

        return mRgba;
    }

    public double getArea(){ return area; }
    public double getDistance() { return distance; }
    public double getNcx() { return ncx; }
    public double getNcy() { return ncy; }
    public double getContourdiff() {return contourdiff; }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols =mRgba.cols();
        int rows =mRgba.rows();

        int xOffset = (v.getWidth() - cols) / 2;
        int yOffset = (v.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        //Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

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


        mDetector.setHsvColor(mBlobColorHsv);

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

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
}
