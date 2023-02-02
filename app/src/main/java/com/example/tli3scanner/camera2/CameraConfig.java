package com.example.tli3scanner.camera2;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.util.Range;
import android.util.Size;

public class CameraConfig {

    private Size mMediaRecorderSize;
    private Size mSurfaceTextureSize;

    private String mCameraId;
    private int mType;

    private Range<Float> mCropRange;
    private Range<Float> mFocusRange;

    private boolean mCameraMulti;
    private float mDistance;
    private long mFrameRate;
    private long mStall;

    private float mMinimumFocus;
    private float mZoomMax;
    private Rect mCrop;
    private float mZoomCurrent;

    private boolean mDiop;
    private long mDuration;

    public CameraConfig(String cameraId, int type){
        mCameraId = cameraId;
        mType = type;
        mMediaRecorderSize = new Size(0,0);
        mSurfaceTextureSize = new Size(0,0);
        mCropRange = new Range<>(0.f,1.f);
    }

    public int getTypeConfig(){
        return mType;
    }

    public void setTypeConfig(int type){
        mType = type;
    }

    public void setRangeCropDistance(Range<Float> range){
        mCropRange = range;
    }

    public Range<Float> getRangeCropDistance(){
        return mCropRange;
    }

    public void setRangeFocusDistance(Range<Float> range){
        mFocusRange = range;
    }

    public Range<Float> getRangeFocusDistance(){
        return mFocusRange;
    }

    public void setCropRect(Rect crop){
        mCrop = crop;
    }

    public Rect getCropRect(){
        return mCrop;
    }

    public void setMultiCamera(boolean multi){
        mCameraMulti = multi;
    }

    public void setFocusDistance(float distance){
        mDistance = distance;
    }

    public float getFocusDistance(){
        return mDistance;
    }

    public float getFocusDistanceInDiop(){
        return 1000/mDistance;
    }

    public void setFrameRate(long frameRate){
        mFrameRate = frameRate;
    }


    public void setDuration(Long duration){
        mDuration = duration;
    }

    public void getDuration(Long duration){
        mDuration = duration;
    }

    public long getFrameRate(){
        return mFrameRate;
    }

    public void setStallDuration(long stall){
        mStall = stall;
    }

    public long getStallDuration(){
        return mStall;
    }

    public int getFrameRateInMili(){
        return (int)(1_000_000_000 / mFrameRate);
    }


    public String getCameraId(){
        return mCameraId;
    }

    public void setEnableDiop(boolean diop){
        mDiop = diop;
    }


    public void setMinimumFocus(float focus){
        mMinimumFocus = focus;
    }

    public float getMinimumFocus(){
        return mMinimumFocus;
    }

    public void setMaxCropZoom(float zoom){
        mZoomMax = zoom;
    }

    public Size getMediaRecorderSize(){
        return mMediaRecorderSize;
    }

    public void setMediaRecorderSize(Size size){ mMediaRecorderSize = size;}

    public Size getSurfaceTextureSize(){
        return mSurfaceTextureSize;
    }

    public void setSurfaceTextureSize(Size size){
        mSurfaceTextureSize = size;
    }

    public void setCurrentZoom(float zoom){
        mZoomCurrent = zoom;
    }

    public float getCurrentZoom(){
        return mZoomCurrent;
    }

    @SuppressLint("DefaultLocale")
    public String toString(){
        /*
            Тип настройки
            Разрешение видео
            Разрешение превью
            ID камеры
            Модель телефона
         */
        String Format = "%d;%s;%s;%s;%s\n";
        return String.format(Format, mType, mMediaRecorderSize.toString(),
                mSurfaceTextureSize.toString(), mCameraId, Build.MODEL);
    }
}
