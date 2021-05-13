package jp.oist.abcvlib.util;

import android.util.Log;

public class ErrorHandler {
    public static void eLog(String TAG, String comment, Exception e, boolean crash){
        Log.e(TAG, comment, e);
        if (crash){
            throw new RuntimeException("This is an intentional debugging crash");
        }
    }
}
