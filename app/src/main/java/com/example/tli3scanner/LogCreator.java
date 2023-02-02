package com.example.tli3scanner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class LogCreator {
    private final int MAX_LOGS_COUNT = 50;

    public static final String INIT_DEVICE_STATE = "Start device initialization;";
    public static final String CHECK_DEVICE_EXIST_STATE = "Checking the availability of the report: %s;";
    public static final String INIT_DEVICE_SEARCH_STATE = "Search for an initialized device: %s;";
    public static final String START_RECORDING_REPORT_STATE = "Start of the report recording;";
    public static final String READY_RECORDING_REPORT_STATE = "The video report is ready to be sent;";
    public static final String SENDING_VIDEO_REPORT_STATE = "Sending a video report...";
    public static final String PROCESSING_RESULT_STATE = "Report processing result: %s";

    public static final String START_CAMERA_ACTIVITY = "Start camera activity;";
    public static final String REFRESH_CAMERA_ACTIVITY = "Refresh camera activity;";

    public static final String INIT_CAMERA_CONFIG_ERROR = "Couldn't find the configuration for the camera!";
    public static final String CONNECTION_CAMERA_ERROR = "Failed to connect to camera!";
    public static final String INIT_CAMERA_SESSION_ERROR = "Failed to start the session!";
    public static final String START_RECORDING_ERROR = "Failed to start recording!";
    public static final String STOP_RECORDING_ERROR = "Failed to stop recording!";
    public static final String REFRESH_RECORDING_ERROR = "Failed to refresh recording!";
    public static final String PREPARE_RECORDING_ERROR = "Failed to prepare recording!";
    public static final String FRAME_CAPTURE_ERROR = "Frame capture error: type=%s, duration=%s, focus=%s, fstate=%s!";

    private LinkedList<String> mLogs;

    LogCreator(){
        mLogs = new LinkedList<>();
    }

    public void addLog(String format, String... args){
        mLogs.add("(" + getDateLog() + ") " + String.format(format, args));
        if(mLogs.size() == MAX_LOGS_COUNT){mLogs.pollFirst();}
    }

    public void clearLogs(){
        mLogs.clear();
    }

    public String getAllLogs(){
        StringBuilder result = new StringBuilder();
        for(String log : mLogs){ result.append(log).append("\n"); }
        return result.toString();
    }

    private String getDateLog(){
        Calendar mCalendar = Calendar.getInstance();
        SimpleDateFormat mFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        return mFormatter.format(mCalendar.getTime());
    }
}
