package com.example.tli3scanner.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class HistoryCreator {
    private final String CASH_NAME = "cash";
    private final String SPLITTER_LINE = ";";
    private final String SPLITTER_VALUE = ",";
    private final String FORMAT_VALUE = "%s" + SPLITTER_VALUE + "%s" + SPLITTER_LINE;
    private final String FORMAT_DATE = "dd-MM-yyyy HH:mm:ss";
    private final int VALUE_LENGTH = 4 + FORMAT_DATE.length() + SPLITTER_VALUE.length() + SPLITTER_LINE.length();

    private LinkedHashMap<String, String> mCashValue;
    private File mCashFile;
    private boolean mDateUpdate = false;

    public HistoryCreator(File fileDir){
        mCashFile = new File(fileDir, CASH_NAME);
        mCashValue = new LinkedHashMap<>();
        loadCashFile(mCashValue);
    }

    public boolean wasUpdateDate(){
        return mDateUpdate;
    }

    public void rewriteCashFile(){
        StringBuilder data = new StringBuilder(mCashValue.size() * VALUE_LENGTH);
        for (Map.Entry<String, String> entry : mCashValue.entrySet()) {
            data.append(String.format(FORMAT_VALUE, entry.getKey(), entry.getValue()));
        }
        mDateUpdate = !writeData(data.toString(), false);
    }


    public void addCashValue(String serial, Date date){
        String strDate = dateToString(date);
        if(mCashValue.containsKey(serial)){
            mDateUpdate = true;
            mCashValue.remove(serial);
        }else{
            writeData(String.format(FORMAT_VALUE, serial, strDate), true);
        }
        mCashValue.put(serial, strDate);
    }

    public void removeCashValue(String serial){
        if(!mCashValue.containsKey(serial)) return;
        mCashValue.remove(serial);
        rewriteCashFile();
    }

    public ArrayList<HistoryItem> getAllHistoryItems(){
        ArrayList<HistoryItem> items = new ArrayList<>(mCashValue.size());
        for (Map.Entry<String, String> entry : mCashValue.entrySet()) {
            items.add(new HistoryItem(entry.getKey(), entry.getValue()));
        }
        Collections.reverse(items);
        return items;

    }

    private void loadCashFile(LinkedHashMap<String, String> map){
        String cash = readAllCashFile();
        if(cash == null || cash.length() == 0) return;
        String[] cashValues = cash.split(SPLITTER_LINE);
        for(String line : cashValues){
            String[] values = line.split(SPLITTER_VALUE);
            map.put(values[0], values[1]);
        }
    }

    private String readAllCashFile(){
        if(!mCashFile.exists()) return null;
        try(FileInputStream fin =  new FileInputStream(mCashFile)) {
            byte[] bytes = new byte[fin.available()];
            fin.read(bytes);
            return new String (bytes);
        }
        catch(IOException ex) {
            return null;
        }
    }

    private boolean writeData(String data, boolean append){
        try(FileOutputStream fos = new FileOutputStream(mCashFile, append)) {
            fos.write(data.getBytes());
        }
        catch(IOException ex) {
            return false;
        }
        return true;
    }

    private String dateToString(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat(FORMAT_DATE);
        return dateFormat.format(date);
    }
}
