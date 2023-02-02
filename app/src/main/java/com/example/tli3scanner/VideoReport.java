package com.example.tli3scanner;

import android.util.Base64;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class VideoReport {

    public static long MAX_MILLISECONDS_VIDEO = 20000;
    public static long MIN_MILLISECONDS_VIDEO = 7000;

    private File mFile;
    private String mSerial;
    private long mTimeStart;
    private File mDirReports;
    private String mBase64Key;

    public VideoReport(File dir, String timeZone, String lang){
        mDirReports = dir;
        String key = lang + ";" + timeZone;
        mBase64Key = Base64.encodeToString(key.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT).trim();
    }

    public File getDirReports(){
        return mDirReports;
    }

    public File getReportPath(){
        return new File(getDirReports(), getPdfName());
    }

    public void setFile(File file){
        mFile = file;
    }

    public String getPdfName(){
        return mSerial + ".pdf";
    }

    public String getKeyValue(){
        return mSerial + mBase64Key;
    }

    public boolean deleteVideoFile(){
        return mFile.delete();
    }

    public void setSerial(String serial){
        mSerial = serial;
    }

    public String getSerial(){
        return mSerial;
    }

    public String getSerial10(){
        String serial10 = Long.valueOf(mSerial, 36).toString();
        StringBuilder sb = new StringBuilder();

        int count = 6 - serial10.length();
        while(count-- != 0){ sb.append('0'); }
        sb.append(serial10);
        sb.insert(3, '-');

        return sb.toString();
    }

    public boolean equalsSerial(String serial){
        return mSerial.equals(serial);
    }

    public File getFile(){
        return mFile;
    }

    public void setStartRecording(long time){
        mTimeStart = time;
    }

    public long getTimeDifference(long time){
        return time - mTimeStart;
    }
}
