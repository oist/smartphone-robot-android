package jp.oist.abcvlib.basic;

import android.os.Environment;
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
public class AbcvlibSaveData {

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

        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }
}
