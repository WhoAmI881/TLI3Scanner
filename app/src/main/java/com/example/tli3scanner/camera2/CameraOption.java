package com.example.tli3scanner.camera2;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraOption {

    public static final int MODE_3A_OFF = 22;
    public static final int MODE_AE_AF_OFF_AWB_LOCK = 21;
    public static final int MODE_AE_AF_OFF_AWB_ON = 20;
    public static final int MODE_AE_AWB_OFF_AF_ON = 18;
    public static final int MODE_AE_OFF_AF_AWB_ON = 16;
    public static final int MODE_AE_LOCK_AF_AWB_OFF = 14;
    public static final int MODE_AE_AWB_LOCK_AF_OFF = 13;
    public static final int MODE_AE_LOCK_AF_OFF_AWB_ON = 12;
    public static final int MODE_AE_LOCK_AF_AWB_ON = 8;
    public static final int MODE_AE_ON_AF_AWB_OFF = 6;
    public static final int MODE_AE_AWB_ON_AF_OFF = 4;
    public static final int MODE_AE_AF_ON_AWB_OFF = 2;
    public static final int MODE_3A_ON = 0;

    private static List<Integer> mTypes = new ArrayList<>(Arrays.asList(
            MODE_3A_OFF,
            MODE_AE_AF_OFF_AWB_LOCK,
            MODE_AE_AF_OFF_AWB_ON,
            MODE_AE_AWB_OFF_AF_ON,
            MODE_AE_OFF_AF_AWB_ON,
            MODE_AE_LOCK_AF_AWB_OFF,
            MODE_AE_AWB_LOCK_AF_OFF,
            MODE_AE_LOCK_AF_OFF_AWB_ON,
            MODE_AE_LOCK_AF_AWB_ON,
            MODE_AE_ON_AF_AWB_OFF,
            MODE_AE_AWB_ON_AF_OFF,
            MODE_AE_AF_ON_AWB_OFF,
            MODE_3A_ON
    ));

    private static final List<Integer> MODE_OFF_AWB = Arrays.asList(
            MODE_3A_OFF,
            MODE_AE_AWB_OFF_AF_ON,
            MODE_AE_LOCK_AF_AWB_OFF,
            MODE_AE_ON_AF_AWB_OFF,
            MODE_AE_AF_ON_AWB_OFF
    );

    private static final List<Integer> MODE_OFF_AE = Arrays.asList(
            MODE_3A_OFF,
            MODE_AE_AF_OFF_AWB_LOCK,
            MODE_AE_AF_OFF_AWB_ON,
            MODE_AE_AWB_OFF_AF_ON,
            MODE_AE_OFF_AF_AWB_ON
    );
    private static final List<Integer> MODE_OFF_AF = Arrays.asList(
            MODE_3A_OFF,
            MODE_AE_AF_OFF_AWB_LOCK,
            MODE_AE_AF_OFF_AWB_ON,
            MODE_AE_LOCK_AF_AWB_OFF,
            MODE_AE_AWB_LOCK_AF_OFF,
            MODE_AE_LOCK_AF_OFF_AWB_ON,
            MODE_AE_ON_AF_AWB_OFF,
            MODE_AE_AWB_ON_AF_OFF
    );

    private static final List<Integer> MODE_LOCK_AE = Arrays.asList(
            CameraOption.MODE_AE_LOCK_AF_AWB_OFF,
            CameraOption.MODE_AE_AWB_LOCK_AF_OFF,
            CameraOption.MODE_AE_LOCK_AF_OFF_AWB_ON,
            CameraOption.MODE_AE_LOCK_AF_AWB_ON
    );

    private static final List<Integer> MODE_LOCK_AWB = Arrays.asList(
            CameraOption.MODE_AE_AF_OFF_AWB_LOCK,
            CameraOption.MODE_AE_AWB_LOCK_AF_OFF
    );


    private CameraCharacteristics mCameraCharacteristics;

    public CameraOption(CameraCharacteristics characteristics){
        mCameraCharacteristics = characteristics;
    }
    public static int nextType(int type){
        int index = mTypes.indexOf(type) + 1;
        if(index != mTypes.size()){ return mTypes.get(index);}
        return mTypes.get(mTypes.size() - 1);
    }

    public static boolean containsModeAEOff(int type){
        return MODE_OFF_AE.contains(type);
    }

    public static boolean containsModeAWBOff(int type){
        return MODE_OFF_AWB.contains(type);
    }

    public static boolean containsModeAFOff(int type){
        return MODE_OFF_AF.contains(type);
    }

    public static boolean containsModeAELock(int type){
        return MODE_LOCK_AE.contains(type);
    }

    public static boolean containsModeAWBLock(int type){
        return MODE_LOCK_AWB.contains(type);
    }

    public int get3AModeCamera(){
        int type = MODE_3A_ON;
        boolean fAE = isAvailableOffAE();
        boolean fAWB = isAvailableOffAWB();

        if(fAE){ type |= 16;}
        if(!fAE && isControlAELockAvailable()){ type |= 8;}
        if(isAvailableOffAF()){ type |= 4;}
        if(fAWB){ type |= 2;}
        if(!fAWB && isControlAWBLockAvailable()){ type |= 1;}

        return type;
    }

    public boolean isBackCamera(){
        return mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK;
    }

    public long getMaxFrameDuration(){
        return mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION);
    }

    public Size getOptimalSize(Class clazz, Size strive_resolution){
        Size[] sizes = getStreamConfigurationMap().getOutputSizes(clazz);
        List<Size> variants = new ArrayList<Size>();
        for(Size size : sizes){
            if(size.getWidth() >= strive_resolution.getWidth()
                    && size.getHeight() >= strive_resolution.getHeight()){
                variants.add(size);
            }
        }
        if(variants.size() > 0){
            return variants.get(variants.size()-1);
        }
        return sizes[0];
    }

    public long getMinFrameDurationByResolution(Class clazz, Size strive_resolution){
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputMinFrameDuration(clazz, strive_resolution);
    }

    private StreamConfigurationMap getStreamConfigurationMap(){
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    }

    public Range<Float> getRangeCropZoomByVisibleArea(float visible_area){
        float minWidth = visible_area;
        float maxWidth = visible_area * getMaxCropZoom();

        float minDistance = getDistanceByCropWidth(minWidth);
        float maxDistance = getDistanceByCropWidth(maxWidth);

        return new Range<>(minDistance, maxDistance);
    }

    public Range<Float> getRangeFocus(){
        float min = getMinimumFocusDistance();
        if(min != 0) {
            float minFocus = 1000 / min;
            float maxFocus = 1000 / getMaximumFocusDistance();
            return new Range<>(minFocus, maxFocus);
        }
        return new Range<>(0.f, 0.1f);
    }

    public Rect getCropRegionByDistanceAndVisibleArea(float distance, float visible_area){
        Rect activeArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        float zoom = getZoomByDistanceAndVisibleArea(distance, visible_area);
        if(((int)zoom) == 0){return activeArraySize;}
        float cropW = activeArraySize.width() / zoom;
        float cropH = activeArraySize.height() / zoom;

        int left = activeArraySize.centerX() - (int) (cropW / 2f);
        int top = activeArraySize.centerY() - (int) (cropH / 2f);
        int right = activeArraySize.centerX() + (int) (cropW / 2f);
        int bottom = activeArraySize.centerY() + (int) (cropH / 2f);

        return new Rect(left, top, right, bottom);
    }

    public int getRotationCamera(){
        return mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }


    private float getZoomByDistanceAndVisibleArea(float distance, float visible_area){
        float[] maxFocus = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        float d1 = maxFocus[0];
        float w1_mm = size.getHeight();
        double ang1 = Math.atan(w1_mm/(d1*2));

        float w2_mm = (float)(Math.tan(ang1)*distance*2);
        return w2_mm/visible_area;
    }


    private float getMaximumFocusDistance(){
        return mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE);
    }
    private float getMaxCropZoom(){
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
    }

    private float getDistanceByCropWidth(float width){
        float[] maxFocus = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        SizeF size = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

        float d1 = maxFocus[0];
        float w1_mm = size.getHeight();
        double ang1 = Math.atan(w1_mm/(d1*2));

        return (float)(width / (Math.tan(ang1)*2));
    }

    private boolean isAvailableOffAWB(){
        for(int mode : mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)){
            if(mode == CameraCharacteristics.CONTROL_AWB_MODE_OFF){
                return true;
            }
        }
        return false;
    }

    private boolean isAvailableOffAE(){
        for(int mode : mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)){
            if(mode == CameraCharacteristics.CONTROL_AE_MODE_OFF){
                return true;
            }
        }
        return false;
    }

    private boolean isAvailableOffAF(){
        boolean Off = false;
        for(int mode : mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)){
            if(mode == CameraCharacteristics.CONTROL_AF_MODE_OFF){
                Off = true;
                break;
            }
        }
        return Off && isDioptersCalibration() && getMinimumFocusDistance() != 0;
    }

    private boolean isDioptersCalibration(){
        return mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
                != CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED;
    }

    private float getMinimumFocusDistance(){
        return mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
    }

    private boolean isControlAELockAvailable(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE);
        }
        return false;
    }

    private boolean isControlAWBLockAvailable(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE);
        }
        return false;
    }


}
