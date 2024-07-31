package jp.oist.abcvlib.basiccharger;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {
    private Paint paint;
    private RectF rectF;
    private Matrix matrix = new Matrix();

    private int previewWidth;
    private int previewHeight;
    private int imageWidth;
    private int imageHeight;

    private int bbleft = 80;
    private int bbright = 0;
    private int bbtop = 0;
    private int bbbottom = 80;
    private int direction = 5;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFFFF0000); // Red color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
    }

    public void setRect(RectF rectF) {
        this.rectF = rectF;
        invalidate(); // Request to redraw the view
    }

    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }

    public void setPreviewDimensions(int width, int height) {
        this.previewWidth = width;
        this.previewHeight = height;
        invalidate(); // Request to redraw the view
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (rectF != null && imageWidth > 0 && imageHeight > 0) {
            // Calculate the scale and translation
//            float scaleX = (float) previewWidth / imageWidth;
            float scaleY = (float) previewHeight / imageHeight;
            float scaleX = scaleY;

            float scale = Math.min(scaleX, scaleY);

            float offsetX = (previewWidth -  imageWidth * scale) / 2;
            float offsetY = (previewHeight - imageHeight * scale) / 2;

//            // Testing coordinates
//            rectF = new RectF(bbleft, bbright, bbtop, bbbottom);
//            if (bbleft >= (this.imageWidth)){
//                direction = -direction;
//            }else if (bbleft <= 70){
//                direction = -direction;
//            }
//            bbleft += direction;
//             Testing coordinates
//            rectF = new RectF(bbleft, bbright, bbtop, bbbottom);
//            if (bbbottom >= (this.imageHeight)){
//                direction = -direction;
//            }else if (bbbottom <= 70){
//                direction = -direction;
//            }
//            bbbottom += direction;

            matrix.reset();
//            matrix.postScale(scale, scale);
//            matrix.postTranslate(offsetX, offsetY);

            matrix.reset();
            // Apply scaling
            matrix.postScale(scale, scale);
            // Apply horizontal flip around the center of the image
            matrix.postScale(-1, 1);
            // Translate back to the correct position after flipping
            matrix.postTranslate(previewWidth, 0);
            // Apply translation to align the image in the PreviewView
            matrix.postTranslate(-offsetX, offsetY);

            RectF transformedRect = new RectF(rectF);
            matrix.mapRect(transformedRect);

            canvas.drawRect(transformedRect, paint);
        }
    }
}
