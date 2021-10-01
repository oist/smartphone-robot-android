package jp.oist.abcvlib.core.inputs.phone;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.Publisher;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ImageOps;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;
import jp.oist.abcvlib.util.YuvToRgbConverter;

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUV_422_888;
import static android.graphics.ImageFormat.YUV_444_888;

public class ImageData extends Publisher<ImageDataSubscriber> implements ImageAnalysis.Analyzer {

    private ImageAnalysis imageAnalysis;
    private YuvToRgbConverter yuvToRgbConverter;
    private PreviewView previewView;
    private final LifecycleOwner lifecycleOwner;

    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    private ProcessCameraProvider cameraProvider;
    private final String TAG = getClass().getName();
    private ExecutorService imageExecutor;
    private final CountDownLatch countDownLatch = new CountDownLatch(2); // Waits for both analysis and preview to be running before sending a signal that it is ready
    private boolean qrcodescanning = false;

    /*
     * @param previewView: e.g. from your Activity findViewById(R.id.camera_x_preview)
     * @param imageAnalysis: If set to null will generate default imageAnalysis object.
     */
    public ImageData(Context context, PublisherManager publisherManager, LifecycleOwner lifecycleOwner, PreviewView previewView,
                     ImageAnalysis imageAnalysis){
        super(context, publisherManager);
        this.lifecycleOwner = lifecycleOwner;
        this.previewView = previewView;
        this.imageAnalysis = imageAnalysis;
    }

    public static class Builder{
        private final Context context;
        private final PublisherManager publisherManager;
        private final LifecycleOwner lifecycleOwner;
        private PreviewView previewView;
        private ImageAnalysis imageAnalysis;

        public Builder(Context context, PublisherManager publisherManager, LifecycleOwner lifecycleOwner){
            this.publisherManager = publisherManager;
            this.context = context;
            this.lifecycleOwner = lifecycleOwner;
        }

        public ImageData build(){
            return new ImageData(context, publisherManager, lifecycleOwner, previewView, imageAnalysis);
        }

        public Builder setPreviewView(PreviewView previewView){
            this.previewView = previewView;
            return this;
        }

        public Builder setImageAnalysis(ImageAnalysis imageAnalysis){
            this.imageAnalysis = imageAnalysis;
            return this;
        }
    }

    @Override
    public ArrayList<String> getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        return permissions;
    }

    public ImageAnalysis getImageAnalysis() {
        return imageAnalysis;
    }

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        countDownLatch.countDown();
        Image image;
        if (subscribers.size() > 0 && !paused){
            image = imageProxy.getImage();
        } else {
            imageProxy.close();
            return;}
        if (image != null) {
            int width = image.getWidth();
            int height = image.getHeight();
            long timestamp = image.getTimestamp();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            yuvToRgbConverter.yuvToRgb(image, bitmap);

            String qrDecodedData = "";
            if (qrcodescanning && (image.getFormat() == YUV_420_888 || image.getFormat() == YUV_422_888 || image.getFormat() == YUV_444_888)) {
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] imageData = new byte[byteBuffer.capacity()];
                byteBuffer.flip();
                byteBuffer.get(imageData);

                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        imageData,
                        image.getWidth(), image.getHeight(),
                        0, 0,
                        image.getWidth(), image.getHeight(),
                        false
                );

                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                try {
                    Result result = new QRCodeReader().decode(binaryBitmap);
                    qrDecodedData = result.getText();
                } catch (FormatException e) {
                    Log.v("qrcode", "QR Code cannot be decoded");
                } catch (ChecksumException e) {
                    Log.v("qrcode", "QR Code error correction failed");
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    Log.v("qrcode", "QR Code not found");
                }
            }

            for (ImageDataSubscriber subscriber:subscribers){
                subscriber.onImageDataUpdate(timestamp, width, height, bitmap, qrDecodedData);
            }
        }
        imageProxy.close();
    }

    private void setDefaultImageAnalysis(){

        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(10, 10))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
    }

    public void setQrcodescanning(boolean bool){
        qrcodescanning = bool;
    }

    @Override
    public void start() {
        if (imageAnalysis == null) {
            setDefaultImageAnalysis();
        }
            imageExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "imageAnalysis"));
        if (imageAnalysis == null && previewView == null){
            throw new UnsupportedOperationException("Either setImageAnalysis or setPreviewView must be called prior to calling the start method");
        }
        if (imageAnalysis != null && subscribers.size() > 0){
            yuvToRgbConverter = new YuvToRgbConverter(context);
            imageAnalysis.setAnalyzer(imageExecutor, this);
        }
        if (previewView != null){
            Handler handler = new Handler(context.getMainLooper());
            handler.post(() -> previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER));
            previewView.post(() -> {
                mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
                mCameraProviderFuture.addListener(() -> {
                    try {
                        cameraProvider = mCameraProviderFuture.get();
                        cameraProvider.unbindAll();
                        bindAll(cameraProvider, lifecycleOwner);
                    } catch (ExecutionException | InterruptedException e) {
                        ErrorHandler.eLog(TAG, "Unexpected Error", e, true);
                    }
                }, ContextCompat.getMainExecutor(context));
            });
        }
        try {
            countDownLatch.await();
            publisherManager.onPublisherInitialized();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        imageAnalysis.clearAnalyzer();
        imageAnalysis = null;
        imageExecutor.shutdown();
        yuvToRgbConverter = null;
        previewView = null;
        mCameraProviderFuture.cancel(false);
        cameraProvider.unbindAll();
        cameraProvider = null;
    }

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner) {
        Preview preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        if (imageAnalysis != null){
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis);
        }else{
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview);
        }
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        final Observer<PreviewView.StreamState> previewViewObserver = new Observer<PreviewView.StreamState>() {
            @Override
            public void onChanged(PreviewView.StreamState streamState) {
                Log.i("previewView", "PreviewState: " + streamState.toString());
                if (streamState.name().equals("STREAMING")){
                    countDownLatch.countDown();
                }
            }
        };
        this.previewView.getPreviewStreamState().observe(lifecycleOwner, previewViewObserver);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    public void test() {
        Log.v("lifecycle", "onAny");
    }
}
