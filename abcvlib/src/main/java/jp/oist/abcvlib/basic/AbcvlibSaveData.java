package jp.oist.abcvlib.basic;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;


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
}
