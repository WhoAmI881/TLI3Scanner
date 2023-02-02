package com.example.tli3scanner.history;

public class HistoryItem {

    private final int SERIAL10_MAX_LENGTH = 6;

    private String mSerial36;
    private String mSerial10;
    private String mDateTime;

    public HistoryItem(String serial, String datetime){
        mSerial36 = serial;
        mSerial10 = createSerial10(serial);
        mDateTime = datetime;
    }

    private String createSerial10(String serial36){
        String serial10 = Long.valueOf(serial36, 36).toString();
        StringBuilder sb = new StringBuilder();

        int count = SERIAL10_MAX_LENGTH - serial10.length();
        while(count-- != 0){ sb.append('0'); }
        sb.append(serial10);
        sb.insert(3, '-');

        return sb.toString();
    }

    public String getSerial10(){
        return mSerial10;
    }

    public String getSerial36(){
        return mSerial36;
    }

    public String getDateTime(){
        return mDateTime;
    }

}
