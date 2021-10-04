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
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.oist.abcvlib.core.inputs.Publisher;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.ProcessPriorityThreadFactory;

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUV_422_888;
import static android.graphics.ImageFormat.YUV_444_888;

public class QRCodeData extends Publisher<QRCodeDataSubscriber> implements ImageAnalysis.Analyzer{

    private ImageAnalysis imageAnalysis;
    private final LifecycleOwner lifecycleOwner;
    private ExecutorService qrExecutor;
    private Context context;
    private ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;

    public QRCodeData(Context context, PublisherManager publisherManager, LifecycleOwner lifecycleOwner,
                      ImageAnalysis imageAnalysis) {
        super(context, publisherManager);
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.imageAnalysis = imageAnalysis;
    }

    public static class Builder{
        private final Context context;
        private final PublisherManager publisherManager;
        private final LifecycleOwner lifecycleOwner;
        private ImageAnalysis imageAnalysis;

        public Builder(Context context, PublisherManager publisherManager, LifecycleOwner lifecycleOwner){
            this.publisherManager = publisherManager;
            this.context = context;
            this.lifecycleOwner = lifecycleOwner;
        }

        public QRCodeData build(){
            return new QRCodeData(context, publisherManager, lifecycleOwner, imageAnalysis);
        }

        public QRCodeData.Builder setImageAnalysis(ImageAnalysis imageAnalysis){
            this.imageAnalysis = imageAnalysis;
            return this;
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(@NonNull @NotNull ImageProxy imageProxy) {
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
            String qrDecodedData = "";
            if (image.getFormat() == YUV_420_888 || image.getFormat() == YUV_422_888 || image.getFormat() == YUV_444_888) {
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] imageData = new byte[byteBuffer.capacity()];
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
                    for (QRCodeDataSubscriber subscriber:subscribers){
                        subscriber.onQRCodeDetected(qrDecodedData);
                    }
                } catch (FormatException e) {
                    Log.v("qrcode", "QR Code cannot be decoded");
                } catch (ChecksumException e) {
                    Log.v("qrcode", "QR Code error correction failed");
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    Log.v("qrcode", "QR Code not found");
                }
            }
        }
        imageProxy.close();
    }

    private void setDefaultImageAnalysis(){
        imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(400, 300))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setImageQueueDepth(20)
                        .build();
    }

    @Override
    public void start() {
        if (imageAnalysis == null) {
            setDefaultImageAnalysis();
        }
        qrExecutor = Executors.newCachedThreadPool(new ProcessPriorityThreadFactory(Thread.MAX_PRIORITY, "qrCodeAnalysis"));
        if (imageAnalysis == null){
            throw new UnsupportedOperationException("Either setImageAnalysis or setPreviewView must be called prior to calling the start method");
        }
        if (subscribers.size() > 0){
            imageAnalysis.setAnalyzer(qrExecutor, this);
        }
        Handler handler = new Handler(context.getMainLooper());
        handler.post(() -> {
            mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
            mCameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = mCameraProviderFuture.get();
//                    cameraProvider.unbindAll();
                    bindAll(cameraProvider, lifecycleOwner);
                } catch (ExecutionException | InterruptedException e) {
                    ErrorHandler.eLog(TAG, "Unexpected Error", e, true);
                }
            }, ContextCompat.getMainExecutor(context));
        });
        publisherManager.onPublisherInitialized();
    }

    @Override
    public void stop() {
        imageAnalysis.clearAnalyzer();
        imageAnalysis = null;
        qrExecutor.shutdown();
        cameraProvider.unbindAll();
        cameraProvider = null;
    }

    private void bindAll(@NonNull ProcessCameraProvider cameraProvider, LifecycleOwner lifecycleOwner) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        if (imageAnalysis != null){
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis);
        }
    }

    @Override
    public ArrayList<String> getRequiredPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        return permissions;
    }
}
