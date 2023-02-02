package com.example.tli3scanner.camera2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ReportCreator {

    private final int QR_SIZE = 30;
    private final int VISIBLE_AREA = QR_SIZE * 3;
    private final Size RESOLUTION_PREVIEW = new Size(320, 240);
    private final Size RESOLUTION_VIDEO = new Size(720, 480);
    private final long FRAME_DURATION = 1_000_000_000 / 30;
    private final float MM_DISTANCE = 150;
    private final float MM_ADD_DISTANCE = 50;

    private CameraCaptureSession.StateCallback mSessionStateCallback;
    private CameraCaptureSession.CaptureCallback mCaptureListener;

    private TextureView mCameraPreview;
    private Context mContext;
    private CameraManager mCameraManager;
    private CameraConfig mCameraConfig;
    private CameraOption mCameraOption;
    private VideoRecorder mVideoRecorder;
    private CameraCaptureSession mSession;
    private CaptureSessionBuilder mSessionBuilder;
    private CameraDevice mCamera;
    private RequestBuilder mRequestBuilder;
    private FrameChecker mFrameChecker;

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;


    public ReportCreator(TextureView textureView){
        mCameraPreview = textureView;
        mContext = textureView.getContext();
        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean isErrorCaptureParams(long duration, float focus_distance, int focus_state){
        return mFrameChecker.isErrorCapture(duration, focus_distance, focus_state);
    }

    public boolean lowerCameraConfigMode(){
        int type = CameraOption.nextType(mCameraConfig.getTypeConfig());
        if(type == mCameraConfig.getTypeConfig()) {
            return true;
        }
        mCameraConfig.setTypeConfig(type);
        mFrameChecker.setAFMode(CameraOption.containsModeAFOff(type));
        mRequestBuilder.initCaptureRequestBuilder(mCameraConfig);
        return refreshRepeatingRequest();
    }

    public boolean connectCamera(CameraDevice.StateCallback stateCallback){

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            if(mCameraConfig == null){
                mCameraConfig = getCameraConfig(mCameraManager);
                mCameraOption = new CameraOption(mCameraManager.getCameraCharacteristics(mCameraConfig.getCameraId()));
                Size preview_size = new Size(mCameraPreview.getWidth(), mCameraPreview.getHeight());
                adaptabilityCameraPreview(mCameraConfig.getSurfaceTextureSize(), preview_size);
                mFrameChecker = new FrameChecker(CameraOption.containsModeAFOff(mCameraConfig.getTypeConfig()),
                        mCameraConfig.getFocusDistanceInDiop(), mCameraConfig.getFrameRate());
            }
            startBackgroundThread();
            mCameraManager.openCamera(mCameraConfig.getCameraId(), stateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            stopBackgroundThread();
            return false;
        }

        return true;
    }

    public boolean initCaptureSession(CameraDevice camera, CameraCaptureSession.StateCallback stateCallback){

        mCamera = camera;
        mSessionStateCallback = stateCallback;
        mVideoRecorder = new VideoRecorder(mContext.getExternalFilesDir(null), mCameraConfig.getMediaRecorderSize());

        try {
            mVideoRecorder.prepareRecording();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mSessionBuilder = new CaptureSessionBuilder();
        mSessionBuilder.setPreviewSurface(initPreviewSurface(mCameraPreview, mCameraConfig.getSurfaceTextureSize()));
        mSessionBuilder.setRecorderSurface(initRecorderSurface(mVideoRecorder));
        try {
            mSessionBuilder.initCaptureSession(mCamera, mSessionStateCallback);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean startRepeatingRequest(CameraCaptureSession session, CameraCaptureSession.CaptureCallback callback){
        mSession = session;
        mCaptureListener = callback;
        try {
            mRequestBuilder = new RequestBuilder(mCamera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD));
            mRequestBuilder.addTargets(mSessionBuilder.getAllSurface());
            mRequestBuilder.initCaptureRequestBuilder(mCameraConfig);
            mSession.setRepeatingRequest(mRequestBuilder.getCaptureRequestBuilder().build(), mCaptureListener, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean onFlashCamera(){
        if(mRequestBuilder == null || mSession == null){ return false; }
        mRequestBuilder.setFlashModeTorch();
        return refreshRepeatingRequest();
    }

    public boolean offFlashCamera(){
        if(mRequestBuilder == null || mSession == null){ return false; }
        mRequestBuilder.setFlashModeOff();
        return refreshRepeatingRequest();
    }

    public boolean startRecording(){
        try {
            mVideoRecorder.startRecording();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if(mRequestBuilder.isAvailableControlLock()){
            mRequestBuilder.lockAvailableControlMode();
            return refreshRepeatingRequest();
        }
        return true;
    }

    public File stopRecording(){
        mVideoRecorder.stopRecording();
        if(mRequestBuilder.isAvailableControlLock()){
            mRequestBuilder.unlockAvailableControlMode();
            if(refreshRepeatingRequest()) { return null; };
        }
        return mVideoRecorder.getCurrentFile();
    }

    public boolean refreshRecording(){
        try {
            mVideoRecorder.resetRecording();
            if(mRequestBuilder.isAvailableControlLock()){
                mRequestBuilder.lockAvailableControlMode();
                refreshRepeatingRequest();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                refreshCaptureSession();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean prepareRecording(){
        try {
            mVideoRecorder.prepareRecording();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                refreshCaptureSession();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int getRotationCamera(){
        return mCameraOption.getRotationCamera();
    }

    public void closeCamera(){
        mVideoRecorder.stopRecording();
        mSession.close();
        mCamera.close();
        stopBackgroundThread();
    }

    public String getCameraConfig(){
        return mCameraConfig.toString();
    }

    public int getTypeCameraMode(){
        return mCameraConfig.getTypeConfig();
    }

    private boolean refreshRepeatingRequest(){
        try {
            mSession.setRepeatingRequest(mRequestBuilder.getCaptureRequestBuilder().build(), mCaptureListener, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean refreshCaptureSession(){
        Surface new_surface = mVideoRecorder.getMediaEncoderSurface();
        Surface old_surface = mSessionBuilder.getRecorderSurface();
        mRequestBuilder.replaceTarget(old_surface, new_surface);
        mSessionBuilder.setRecorderSurface(new_surface);
        try {
            mSessionBuilder.initCaptureSession(mCamera, mSessionStateCallback);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Surface initPreviewSurface(TextureView textureView, Size buffer){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(buffer.getWidth(), buffer.getHeight());
        return new Surface(surfaceTexture);
    }

    private Surface initRecorderSurface(VideoRecorder videoRecorder){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return videoRecorder.getInputSurface();
        }else{
            return videoRecorder.getMediaEncoderSurface();
        }
    }

    private void adaptabilityCameraPreview(Size surface_size, Size texture_size){

        float width = surface_size.getWidth();
        float height = surface_size.getHeight();

        float cameraAspectRatio = height/width;

        int screenWidth = texture_size.getWidth();;
        int screenHeight = texture_size.getHeight();

        int finalWidth = screenWidth;
        int finalHeight = screenHeight;
        int widthDifference = 0;
        int heightDifference = 0;
        float screenAspectRatio = (float) screenWidth / screenHeight;

        if (screenAspectRatio > cameraAspectRatio) {
            finalHeight = (int)(screenWidth / cameraAspectRatio);
            heightDifference = finalHeight - screenHeight;
        } else {
            finalWidth = (int)(screenHeight * cameraAspectRatio);
            widthDifference = finalWidth - screenWidth;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mCameraPreview.getLayoutParams();

        lp.width = finalWidth;
        lp.height = finalHeight;

        int lrMargin = - (widthDifference / 2);
        int tbMargin = - (heightDifference / 2);

        lp.leftMargin = lrMargin;
        lp.rightMargin = lrMargin;

        lp.topMargin = tbMargin;
        lp.bottomMargin = tbMargin;

        mCameraPreview.setLayoutParams(lp);
    }

    private void startBackgroundThread() {
        if(mBackgroundHandlerThread != null){
            stopBackgroundThread();
        }
        mBackgroundHandlerThread = new HandlerThread("ReportCreator");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        if(mBackgroundHandlerThread == null){return;}
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private CameraConfig getCameraConfig(CameraManager cameraManager) throws CameraAccessException {
        List<CameraConfig> configs = null;
        configs = getAllEquivalentCameraConfigs(cameraManager);
        return filterConfigs(configs);
    }

    private List<CameraConfig> getAllEquivalentCameraConfigs(CameraManager cameraManager) throws CameraAccessException {
        List<CameraConfig> configs = new ArrayList<>();
        for (String cameraId : cameraManager.getCameraIdList()) {

            CameraOption option = new CameraOption(cameraManager.getCameraCharacteristics(cameraId));

            if(!option.isBackCamera() || option.getMaxFrameDuration() < FRAME_DURATION){ continue; }
            Size mediaSize = option.getOptimalSize(MediaRecorder.class, RESOLUTION_VIDEO);

            long minFrame = option.getMinFrameDurationByResolution(MediaRecorder.class, mediaSize);
            if(minFrame > FRAME_DURATION){ continue; }

            Size surfaceSize = option.getOptimalSize(SurfaceTexture.class, RESOLUTION_PREVIEW);
            int type = option.get3AModeCamera();

            Range<Float> cropRange = option.getRangeCropZoomByVisibleArea(VISIBLE_AREA);
            Range<Float> focusRange = option.getRangeFocus();
            if(CameraOption.containsModeAFOff(type)){
                cropRange = cropRange.intersect(focusRange);
            }

            float distance = MM_DISTANCE;
            if(cropRange.getUpper() < MM_DISTANCE){
                distance = cropRange.getUpper();
            }else if(cropRange.getLower() > MM_DISTANCE){
                distance = cropRange.getLower();
            }

            Rect crop = option.getCropRegionByDistanceAndVisibleArea(distance, VISIBLE_AREA);
            CameraConfig config = new CameraConfig(cameraId, type);

            config.setMediaRecorderSize(mediaSize);
            config.setSurfaceTextureSize(surfaceSize);
            config.setRangeCropDistance(cropRange);
            config.setRangeFocusDistance(focusRange);
            config.setFocusDistance(distance + MM_ADD_DISTANCE);
            config.setCropRect(crop);
            config.setFrameRate(FRAME_DURATION);

            /*
            config.setMinimumFocus(option.getMinimumFocusDistance());
            config.setMaxCropZoom(option.getMaxCropZoom());
            config.setEnableDiop(option.isDioptersCalibration());
            config.setCurrentZoom(option.getZoomByDistance(distance));
             */

            configs.add(config);
        }

        return configs;
    }

    private CameraConfig filterConfigs(List<CameraConfig> configs){
        if(configs.size() == 0){ return null; }

        int max = CameraOption.MODE_3A_ON;
        for(CameraConfig config : configs){
            int type = config.getTypeConfig();
            if(type > max){ max = type;}
        }

        List<CameraConfig> filter = new LinkedList<>();
        for(CameraConfig config : configs){
            if(config.getTypeConfig() == max){
                filter.add(config);
            }
        }

        if(filter.size() == 1){
            return filter.get(0);
        }

        CameraConfig mConfig = filter.get(0);
        for(int idx = 1; idx < filter.size(); idx++){
            Size cMedia = filter.get(idx).getMediaRecorderSize();
            Size mMedia = mConfig.getMediaRecorderSize();
            if(mMedia.getWidth() > cMedia.getWidth() || mMedia.getHeight() > cMedia.getHeight()){
                mConfig = filter.get(idx);
            }
        }

        return mConfig;
    }


}
