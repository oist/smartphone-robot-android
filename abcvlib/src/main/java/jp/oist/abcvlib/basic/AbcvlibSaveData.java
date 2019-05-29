package jp.oist.abcvlib.basic;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Not used anywhere else in the library at this time. Leaving for possible future use.
 */
public class AbcvlibSaveData extends AbcvlibActivity{

    String saved="saved!";

    public void savedata(String content, String filename){

        final String FILENAME=filename;
        //final String FILENAME="datala.txt";
        try{

            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){

                File sdCardDir=Environment.getExternalStorageDirectory();
                if(sdCardDir.exists()){

                    if(sdCardDir.canWrite()){
                        File file=new File(sdCardDir.getAbsolutePath()+"/DataDir");
                        file.mkdir();
                    }
                }
                String filepath=sdCardDir.getAbsolutePath()+"/DataDir/"+FILENAME;
                BufferedWriter bw=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath,false),"UTF-8"));
                bw.write(content);
                bw.close();



            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public double[] readData(String csvString){

        String line = "";
        String[] lineArray;
        double[] output = new double[3];

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(csvString));
            line = bufferedReader.readLine();
            lineArray = line.split(",");
            for (int i = 0; i < lineArray.length; i++){
                output[i] = Double.parseDouble(lineArray[i]);
            }

        } catch (NullPointerException e){

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("abcvlib", "File read failed: " + e.toString());

        }
        return output;
    }

    public void writeToFile(Context context, String savePath, double[] data) {

        String androidDataString = "";
        File file = new File(savePath);

        for (int i = 0; i < data.length; i++){
            androidDataString = androidDataString.concat(data[i] + ",");
        }

        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(androidDataString.getBytes());
        }
        catch (IOException e) {
            Log.e("abcvlib", "File write failed: " + e.toString());
        }finally {
            stream.close();
        }
    }
}
