package com.example.tli3scanner.camera2;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.telecom.VideoProfile;
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.IOException;

public class VideoRecorder {

    private MediaRecorder mMediaRecorder;
    private boolean mRecordingState = false;
    private boolean mFileState = false;
    private Surface mInputSurface;
    private File mFile;

    private final Size mVideoSize;
    private final File mDir;

    public VideoRecorder(File dir, Size videoSize){
        mDir = dir;
        mVideoSize = videoSize;
    }

    public void prepareRecording() throws IOException {
        updateFileName();
        initMediaRecorder();
        prepareMediaRecorder();
    }

    public void startRecording() throws IOException {
        if(mRecordingState){ stopMediaRecorder(); }
        if(mMediaRecorder == null){
            initMediaRecorder();
            prepareMediaRecorder();
        }
        mRecordingState = true;
        mMediaRecorder.start();
    }

    public File stopRecording(){
        if(mRecordingState){ stopMediaRecorder(); }
        if(mMediaRecorder != null) {resetMediaRecorder();}
        return mFile;
    }

    public void resetRecording() throws IOException {
        initMediaRecorder();
        prepareMediaRecorder();
    }

    public Surface getMediaEncoderSurface(){
        return mMediaRecorder.getSurface();
    }

    public Surface getInputSurface(){
        return mInputSurface;
    }

    public File getCurrentFile(){
        return mFile;
    }

    private void prepareMediaRecorder() throws IOException {
        if(mMediaRecorder == null){return;}
        mMediaRecorder.prepare();
    }

    private void resetMediaRecorder(){
        if(mMediaRecorder == null){ return; }
        mMediaRecorder.release();
        mMediaRecorder = null;
    }

    private void initMediaRecorder(){
        if(mRecordingState){stopMediaRecorder();}
        if(mMediaRecorder != null) {resetMediaRecorder();}

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(mInputSurface == null) {
                mInputSurface = MediaCodec.createPersistentInputSurface();
            }
            mMediaRecorder.setInputSurface(mInputSurface);
        }
        mMediaRecorder.setOutputFile(mFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(1_000_000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setOrientationHint(90);
    }

    private void updateFileName(){
        String name = mFileState + ".mp4";
        mFile = new File(mDir, name);
        mFileState = !mFileState;
    }

    private void stopMediaRecorder(){
        if(mMediaRecorder != null){ mMediaRecorder.stop(); }
        mRecordingState = false;
    }
    /*
        1. PrepareRecording
            1.1) если съемка идет:
                    остановить съемку.
                    удалить файл.
            1.2) сбросить MediaRecorder, если он создан. (!= null)
            1.3) обновить имя файла.
            1.4) инициализировать MediaRecorder
            1.5) подготовить к использованию (Prepare)

        2. StartRecording
            2.1) если съемка уже идет:
                    остановить съемку.
                    удалить файл.

            2.2) если MediaRecorder == null:
                    инициализировать MediaRecorder
                    подготовить к использованию (Prepare)
            2.3) Начать сьемку.

       3. StopRecording
            3.1) Остановить съемку, если она есть.
            3.2) Если MediaRecorder != null
                    Сбросить MediaRecorder.
            3.3) Вернуть файл.

       4. RefreshRecording
            4.1) Остановить съемку, если она есть.
            4.2) Если MediaRecorder != null
                    Сбросить MediaRecorder.
            4.3) Инициализировать MediaRecorder
            4.4) Подготовить к использованию (Prepare)

     */

    /*
    private class MediaRecorderPrepare extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mMediaRecorder.prepare();
                mPrepareState = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
     */

}

