package jp.oist.abcvlib.pidtransfer_transmitter;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Display;
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

import jp.oist.abcvlib.util.ErrorHandler;
import jp.oist.abcvlib.util.PID_GUI;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QRCodeDisplay#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QRCodeDisplay extends Fragment {

    private ImageView qrCode;
    private PID_GUI pid_gui;
    private final String TAG = this.getClass().toString();

    public QRCodeDisplay() {
        // Required empty public constructor
    }

    public QRCodeDisplay(PID_GUI pid_gui) {
        this.pid_gui = pid_gui;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment QRCodeDisplay.
     */
    // TODO: Rename and change types and number of parameters
    public static QRCodeDisplay newInstance(PID_GUI pid_gui) {
        QRCodeDisplay fragment = new QRCodeDisplay(pid_gui);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_q_r_code_display, container, false);
        qrCode = rootView.findViewById(R.id.qrView);

        WindowManager wm = requireActivity().getWindowManager();
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        String barcode_data = pid_gui.getControls();
//        barcode_data = "123";

        // barcode image
        Bitmap bitmap = null;

        try {
            //Todo get actual screen size to enlarge this
            bitmap = encodeAsBitmap(barcode_data, BarcodeFormat.QR_CODE, width, width);
            qrCode.setImageBitmap(bitmap);

        } catch (WriterException e) {
            ErrorHandler.eLog(TAG, "Error", e, true);
        }

        return rootView;
    }

    Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int img_width, int img_height) throws WriterException {
        String contentsToEncode = contents;
        if (contentsToEncode == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = "UTF-8";
        hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, encoding);
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result;
        try {
            result = writer.encode(contentsToEncode, format, img_width, img_height, hints);
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