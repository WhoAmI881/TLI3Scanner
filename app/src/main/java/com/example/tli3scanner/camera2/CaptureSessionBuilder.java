package com.example.tli3scanner.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;

public class CaptureSessionBuilder {

    private Surface mPreviewSurface;
    private Surface mRecorderSurface;

    public CaptureSessionBuilder(){

    }

    public void setPreviewSurface(Surface surface){
        mPreviewSurface = surface;
    }

    public Surface getPreviewSurface(){
        return mPreviewSurface;
    }

    public void setRecorderSurface(Surface surface){
        mRecorderSurface = surface;
    }

    public Surface getRecorderSurface(){
        return mRecorderSurface;
    }

    public List<Surface> getAllSurface(){
        return getSurfaceAsList();
    }

    public void initCaptureSession(CameraDevice camera, CameraCaptureSession.StateCallback stateCallback) throws CameraAccessException {
        camera.createCaptureSession(getSurfaceAsList(), stateCallback, null);
    }

    private List<Surface> getSurfaceAsList(){
        List<Surface> lstSurface = new ArrayList<>();
        lstSurface.add(mRecorderSurface);
        lstSurface.add(mPreviewSurface);
        return lstSurface;
    }

}
