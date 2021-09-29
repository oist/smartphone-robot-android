package jp.oist.abcvlib.fragments;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

import jp.oist.abcvlib.core.R;
import jp.oist.abcvlib.util.ErrorHandler;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

public class QRCodeDisplayFragment extends Fragment {

    private final String TAG = this.getClass().toString();
    private final String data2Encode;

    public QRCodeDisplayFragment(String data2Encode) {
        super(R.layout.q_r_code_display);
        this.data2Encode = data2Encode;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.q_r_code_display, container, false);
        ImageView qrCode = rootView.findViewById(R.id.qrImage);
        WindowManager wm = requireActivity().getWindowManager();
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        int width = size.x;
        int height = size.y;
        try {
            Bitmap bitmap = encodeAsBitmap(data2Encode, width, height);
            qrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            ErrorHandler.eLog(TAG, "encodeAsBitmap threw WriterException", e, true);
        }
        return rootView;
    }

    Bitmap encodeAsBitmap(String contents, int img_width, int img_height) throws WriterException {
        if (contents == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints;
        String encoding = "UTF-8";
        hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, encoding);
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contents, BarcodeFormat.QR_CODE, img_width, img_height, hints);
        } catch (IllegalArgumentException iae) {
            Log.d(TAG, "Unsupported format", iae);
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}