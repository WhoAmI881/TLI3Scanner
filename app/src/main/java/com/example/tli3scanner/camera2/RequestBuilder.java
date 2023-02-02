package com.example.tli3scanner.camera2;

import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import android.view.Surface;

import java.util.List;

public class RequestBuilder {

    private CaptureRequest.Builder mCaptureRequestBuilder;
    private boolean mLockAE;
    private boolean mLockAWB;


    public RequestBuilder(CaptureRequest.Builder builder){
        mCaptureRequestBuilder = builder;
    }

    public void addTargets(List<Surface> surfaces){
        for(Surface surface : surfaces) {
            mCaptureRequestBuilder.addTarget(surface);
        }
    }

    public void initCaptureRequestBuilder(CameraConfig config){
        setupCaptureRequestBuilder(mCaptureRequestBuilder, config);
    }

    public CaptureRequest.Builder getCaptureRequestBuilder(){
        return mCaptureRequestBuilder;
    }

    public boolean isAvailableControlLock(){
        return mLockAE || mLockAWB;
    }

    public void lockAvailableControlMode(){
        if(mLockAE){ mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, true); }
        if(mLockAWB){ mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true); }
    }

    public void unlockAvailableControlMode(){
        if(mLockAE){ mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false); }
        if(mLockAWB){ mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, false); }
    }

    public void setFlashModeTorch(){
        mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
    }

    public void setFlashModeOff(){
        mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);
    }

    private void setupCaptureRequestBuilder(CaptureRequest.Builder builder, CameraConfig config){

        int type = config.getTypeConfig();

        // (Баланс белого) CONTROL_AWB_AVAILABLE_MODES
        if(CameraOption.containsModeAWBOff(type)) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF);
        }else{
            builder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
        }

        // (Настройка экспозиции) CONTROL_AE_AVAILABLE_MODES
        if(CameraOption.containsModeAEOff(type)) {
            long frameRate = config.getFrameRate();
            long exposure = (long)(frameRate * 0.7f);
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure);
            builder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameRate);
            builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF);
        }else{
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        }

        // (Авто фокус) CONTROL_AF_AVAILABLE_MODES
        if(CameraOption.containsModeAFOff(type)){
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, config.getFocusDistanceInDiop());
        }else{
            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
        }

        mLockAE = CameraOption.containsModeAELock(type);
        mLockAWB =  CameraOption.containsModeAWBLock(type);

        int frameMili = config.getFrameRateInMili();
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(frameMili, frameMili));
        builder.set(CaptureRequest.SCALER_CROP_REGION, config.getCropRect());
    }

    public void replaceTarget(Surface remove, Surface add){
        mCaptureRequestBuilder.removeTarget(remove);
        mCaptureRequestBuilder.addTarget(add);
    }

}
