package jp.oist.abcvlib.core.inputs.phone.vision;

import android.util.Size;

import androidx.camera.core.ImageAnalysis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

public class ImageData{

    private ImageAnalysis imageAnalysis;
    private final ExecutorService imageExecutor;
    private ImageAnalyzer imageAnalyzer;

    public ImageData(AbcvlibActivity abcvlibActivity){

        imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));
        imageAnalyzer = new ImageAnalyzer(abcvlibActivity);

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
        imageAnalysis.setAnalyzer(imageExecutor, imageAnalyzer);
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }

    public ImageAnalyzer getImageAnalyzer(){return imageAnalyzer;}
}
