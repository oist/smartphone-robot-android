package jp.oist.abcvlib.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Not used anywhere else in the library at this time. Leaving for possible future use.
 */
public class FileOps {
    private static final String TAG = "FileOps";

    public static void savedata(String content, String filename){
        try{
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File sdCardDir=Environment.getExternalStorageDirectory();
                if(sdCardDir.exists() && sdCardDir.canWrite()){
                    File file=new File(sdCardDir.getAbsolutePath() + "/DataDir" + filename);
                    boolean madeDir = file.mkdir();
                    if (madeDir){
//                        String filepath = file.getAbsolutePath() + filename;
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath(),false), StandardCharsets.UTF_8));
                        bw.write(content);
                        bw.close();
                    } else{
                        Log.d(TAG, "Unable to create DataDir directory");
                    }
                }
            }
        }catch(Exception e){
            Log.e(TAG,"Error", e);
        }
    }

    public static void savedata(Context context, byte[] content, String filename){
        try{
            boolean deleted = false;
            boolean created = false;
            File file=new File(context.getFilesDir() + File.separator + filename);
            if (file.exists()){
                deleted = file.delete();
            }
            if (!file.exists() || deleted){
                created = file.createNewFile();
                Log.v(TAG, "Writing " + file.getAbsolutePath());
                FileOutputStream fileOutputStream = new FileOutputStream(file.getAbsolutePath(),false);
                fileOutputStream.write(content);
                fileOutputStream.close();
            }
        }catch(Exception e){
            Log.e(TAG,"Error", e);
        }
    }

    public double[] readData(String fileName){

        String line = "";
        String[] lineArray;
        double[] output = new double[4];
        File file = getFile(fileName);
        String filePath = file.getPath();

        if (isExternalStorageReadable()){
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
                line = bufferedReader.readLine();
                lineArray = line.split(",");
                for (int i = 0; i < lineArray.length; i++){
                    output[i] = Double.parseDouble(lineArray[i]);
                }

            } catch (NullPointerException e){

            } catch (IOException e) {
                Log.e(TAG,"Error", e);
                Log.e("abcvlib", "File read failed: " + e.toString());

            }
        }

        return output;
    }

    public void writeToFile(Context context, String fileName, double[] data) {

        String androidDataString = "";
        File file = getFile(fileName);

        for (int i = 0; i < data.length; i++){
            androidDataString = androidDataString.concat(data[i] + ",");
        }

        if (isExternalStorageWritable()){
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.e(TAG,"Error", e);
            }

            try {
                stream.write(androidDataString.getBytes());
            }
            catch (IOException e) {
                Log.e("abcvlib", "File write failed: " + e.toString());
            }finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG,"Error", e);
                }
            }
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getFile(String fileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            Log.e(TAG,"Error", e);
        }
        return file;
    }

}
