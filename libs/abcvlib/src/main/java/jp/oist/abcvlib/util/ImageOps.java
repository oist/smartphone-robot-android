package jp.oist.abcvlib.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ImageOps {
    public static Bitmap compressImage(Bitmap image, Bitmap.CompressFormat format, int quality){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(format, quality, baos);
        int options = 100;
        ByteArrayInputStream bais = new ByteArrayInputStream(
                baos.toByteArray());
        return BitmapFactory.decodeStream(bais, null, null);
    }

    public static Bitmap generateBitmap(byte[] image){
        ByteArrayInputStream bais = new ByteArrayInputStream(image);
        return BitmapFactory.decodeStream(bais, null, null);
    }
}
