package com.example.tli3scanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.tli3scanner.camera2.ReportCreator;
import com.example.tli3scanner.history.HistoryCreator;
import com.example.tli3scanner.request.YandexCloudRequest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE = 10;

    private final int PERMISSION_FALSE = 0;
    private final int PERMISSION_CHECK = 1;
    private final int PERMISSION_TRUE = 2;

    private final int ERROR_PROCESSING = R.string.error_processing;
    private final int ERROR_SENDING = R.string.video_not_sending;
    private final int ERROR_DOWNLOAD = R.string.report_not_download;
    private final int SUCCESS_RESULT = R.string.success_processing;

    private int mCameraPermission = PERMISSION_TRUE;
    private boolean mFlashState = false;
    private boolean mInitConnection = false;
    private ReportCreator mReportCreator;
    private QRCodeScanner mQRScanner;
    private VideoReport mVideoReport;
    private VideoReportProcessing mVideoReportTask;
    private LogCreator mLogs;
    private HistoryCreator mHistoryCreator;
    private Handler mHandler;

    private DrawerLayout mDrawerLayout;
    private TextureView mCameraPreview;
    private TextView mInfoView;
    private TextView mSerialView;
    private ImageView mImageFlash;
    private ImageView mImageQrView;
    private ImageView mImageHistory;
    private ImageView mImageMenu;
    private Button mOpenButton;
    private ProgressBar mProgressRecording;
    private ProgressBar mProgressVideoSend;

    private OnSuccessListener<List<Barcode>> initQRReport;
    private OnSuccessListener<List<Barcode>> findQRReport;
    private OnSuccessListener<List<Barcode>> recordQRReport;
    private OnSuccessListener<List<Barcode>> readyQRReport;

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            connectCameraDevice();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

        }
    };
    private CameraDevice.StateCallback cameraInitStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            if(!mReportCreator.initCaptureSession(camera, captureSessionStateCallback)){
                errorHandler(LogCreator.CONNECTION_CAMERA_ERROR);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            errorHandler(LogCreator.CONNECTION_CAMERA_ERROR);
        }
    };
    private CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if(!mReportCreator.startRepeatingRequest(session, mCaptureListener)){
                errorHandler(LogCreator.INIT_CAMERA_SESSION_ERROR);
                return;
            }
            mInitConnection = true;
            mQRScanner.enableScanner();
            mQRScanner.setDelay(2000);
            mQRScanner.setSuccessListener(initQRReport);
            mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };
    private CameraCaptureSession.CaptureCallback mCaptureListener = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            long duration = result.get(CaptureResult.SENSOR_FRAME_DURATION);
            float focus_distance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
            int focus_state = result.get(CaptureResult.CONTROL_AF_STATE);

            if(!mReportCreator.isErrorCaptureParams(duration, focus_distance, focus_state)) return;

            mLogs.addLog(LogCreator.FRAME_CAPTURE_ERROR,
                        Integer.toString(mReportCreator.getTypeCameraMode()),
                        Long.toString(duration),
                        Float.toString(focus_distance),
                        Integer.toString(focus_state)
                );

            mReportCreator.lowerCameraConfigMode();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.menuFeedback:
                        openFeedBackIntent();
                        break;
                    case R.id.menuInformation:
                        openInformationIntent();
                        break;
                    case R.id.menuLanguage:
                        /*
                        LanguagePopup popUpClass = new LanguagePopup(findViewById( R.id.menuLanguage).getContext());
                        popUpClass.showLanguageWindow();
                         */
                        changeLanguage();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        mDrawerLayout = findViewById(R.id.drawerLayout);
        mCameraPreview = findViewById(R.id.previewView);
        mImageFlash = findViewById(R.id.flash);
        mImageQrView = findViewById(R.id.viewQR);
        mImageMenu = findViewById(R.id.imageMenu);
        mInfoView = findViewById(R.id.info);
        mOpenButton = findViewById(R.id.open);
        mProgressRecording = findViewById(R.id.progress_recording);
        mSerialView = findViewById(R.id.serial);
        mProgressVideoSend = findViewById(R.id.progress_video_send);
        mImageHistory = findViewById(R.id.history);

        mImageFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mFlashState){
                    mReportCreator.onFlashCamera();
                    mImageFlash.setImageResource(R.drawable.light_on);
                }else{
                    mReportCreator.offFlashCamera();
                    mImageFlash.setImageResource(R.drawable.light_off);
                }
                mFlashState = !mFlashState;
            }
        });
        mImageHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HistoryPopup popUpClass = new HistoryPopup(v.getContext());
                popUpClass.showHistoryWindow(mHistoryCreator.getAllHistoryItems());
                mImageHistory.setImageResource(R.drawable.ic_history_off);
            }
        });
        mImageMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        mReportCreator = new ReportCreator(mCameraPreview);
        mVideoReport = new VideoReport(this.getFilesDir(), TimeZone.getDefault().getID(), getResources().getString(R.string.report_lang));
        mHistoryCreator = new HistoryCreator(this.getFilesDir());
        mLogs = new LogCreator();
        initQRCodeScanner(mImageQrView);
        mHandler = new Handler(Looper.getMainLooper());

        openViewEvent();
        if(!hasCameraPermission()){ mCameraPermission = PERMISSION_FALSE; }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
                ;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.INTERNET
                },
                REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        mCameraPermission = PERMISSION_CHECK;
        if(requestCode != REQUEST_CODE || grantResults.length == 0){
            return;
        }

        for(int grant : grantResults){
            if(grant != PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
        mCameraPermission = PERMISSION_TRUE;;
    }

    @Override
    protected void onResume() {
        super.onResume();

        switch (mCameraPermission){
            case PERMISSION_FALSE:
                requestPermission();
                return;
            case PERMISSION_CHECK:
                checkPermissionEvent();
                return;
            case PERMISSION_TRUE:
                if(!isWifiConnection()){
                    connectWifiEvent();
                    return;
                }
                startCameraConnectionEvent();
        }
    }

    private boolean isWifiConnection(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        return connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;

    }

    private void connectCameraDevice(){
        initDeviceEvent();
        mReportCreator.connectCamera(cameraInitStateCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mCameraPermission == PERMISSION_CHECK || !mInitConnection){
            return;
        }

        if(mVideoReportTask != null && mVideoReportTask.getStatus() == AsyncTask.Status.RUNNING){
            mVideoReportTask.cancel(false);
        }

        mQRScanner.disableScanner();
        mReportCreator.closeCamera();
        mImageFlash.setImageResource(R.drawable.light_off);
        mInitConnection = false;
        if(mHistoryCreator.wasUpdateDate()){ mHistoryCreator.rewriteCashFile(); }
    }

    private void initQRCodeScanner(ImageView qrView){
        mQRScanner = new QRCodeScanner(qrView, new Handler(Looper.getMainLooper()));

        OnFailureListener failureListener = new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
            }
        };

        mQRScanner.setFailureListener(failureListener);

        initQRReport = new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                String serial = mQRScanner.getSerialFromBarcode(barcodes, true);
                if(serial == null){
                    mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
                    return;
                }
                checkExistDeviceEvent(serial);
                new CheckExistReportRequest().execute(serial);
            }
        };

        findQRReport = new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                String serial = mQRScanner.getSerialFromBarcode(barcodes, true);
                if(serial == null){
                    mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
                    return;
                }
                if(!mVideoReport.equalsSerial(serial)){
                    mVideoReport.setSerial(serial);
                    checkExistDeviceEvent(serial);
                    new CheckExistReportRequest().execute(serial);
                    return;
                }

                startRecordReportEvent();
                mVideoReport.setStartRecording(System.currentTimeMillis());
                mQRScanner.setSuccessListener(recordQRReport);
                mQRScanner.setDelay(500);
                mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
                if(!mReportCreator.startRecording()){ errorHandler(LogCreator.START_RECORDING_ERROR);}
            }
        };

        recordQRReport = new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                String Serial = mQRScanner.getSerialFromBarcode(barcodes, false);
                if(Serial == null){
                    if(!mReportCreator.refreshRecording()){
                        errorHandler(LogCreator.REFRESH_RECORDING_ERROR);
                        return;
                    }
                    searchInitDeviceEvent(mVideoReport.getSerial());
                    mQRScanner.setSuccessListener(findQRReport);
                    mQRScanner.setDelay(2000);
                }else {
                    long timeRecord = mVideoReport.getTimeDifference(System.currentTimeMillis());
                    int processing = (int) (timeRecord * 100 / VideoReport.MIN_MILLISECONDS_VIDEO);
                    if (processing < 100) {
                        mProgressRecording.setProgress(processing);
                    } else {
                        mQRScanner.setSuccessListener(readyQRReport);
                        readyVideoReportEvent();
                    }
                }
                mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
            }
        };

        readyQRReport = new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(List<Barcode> barcodes) {
                String Serial = mQRScanner.getSerialFromBarcode(barcodes, false);
                if(Serial != null && mVideoReport.getTimeDifference(System.currentTimeMillis()) < VideoReport.MAX_MILLISECONDS_VIDEO){
                    mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
                    return;
                }
                mQRScanner.resetSizeViewQR();
                File report = mReportCreator.stopRecording();
                if(report == null){
                    errorHandler(LogCreator.STOP_RECORDING_ERROR);
                    return;
                }
                mVideoReport.setFile(report);
                if(!mReportCreator.prepareRecording()){errorHandler(LogCreator.PREPARE_RECORDING_ERROR);}
                sendVideoReportEvent();
                mVideoReportTask = new VideoReportProcessing();
                mVideoReportTask.execute(mVideoReport);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInfoView.setText(R.string.error_wifi_timeout);
                    }
                }, 8000);
            }
        };
    }

    private class CheckExistReportRequest extends AsyncTask<String, Void, Integer> {

        private String mSerial;
        @Override
        protected Integer doInBackground(String... serial) {
            mSerial = serial[0];
            return YandexCloudRequest.thisReportExists(mSerial);
        }

        @Override
        protected void onPostExecute(Integer result){
            if(result == HttpsURLConnection.HTTP_BAD_REQUEST && !isWifiConnection()){
                refreshWifiConnection();
                return;
            }

            mOpenButton.setVisibility(View.INVISIBLE);
            searchInitDeviceEvent(mSerial);
            mVideoReport.setSerial(mSerial);
            mQRScanner.setSuccessListener(findQRReport);
            mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());

            /*
            if(result){
                OpenPdfIntent(Serial);
            }else{
                mReport = new VideoReport(Serial);
                mQRReader.setSuccessListener(findQRReport);
                searchQRRectEvent();
                mQRReader.startScanning(mTextureView.getBitmap(), mVideoCreater.getRotationCamera());
            }
             */

        }

    }

    private class VideoReportProcessing extends AsyncTask<VideoReport, Void, Integer> {

        private VideoReport mSendReport;
        private String mResult;

        @Override
        protected Integer doInBackground(VideoReport... reports) {
            mSendReport = reports[0];
            int StatusCode = YandexCloudRequest.sendVideoInBucket(
                    mSendReport.getKeyValue(),
                    mSendReport.getFile()
            );
            mSendReport.deleteVideoFile();
            if(StatusCode == HttpsURLConnection.HTTP_BAD_REQUEST) { return ERROR_SENDING; }

            mResult = YandexCloudRequest.getResultProcessingVideo(mSendReport.getKeyValue());
            if(mResult == null) { return ERROR_SENDING; }
            if(!mResult.isEmpty()){ return ERROR_PROCESSING; }

            if (!YandexCloudRequest.downloadPDFFile(mSendReport.getPdfName(), mSendReport.getReportPath())){
                return ERROR_DOWNLOAD;
            }
            return SUCCESS_RESULT;
        }

        @Override
        protected void onPostExecute(Integer result){
            mHandler.removeCallbacksAndMessages(null);
            getResultProcessingEvent(result, mResult);
            mQRScanner.setSuccessListener(findQRReport);
            if(result == SUCCESS_RESULT){
                mHistoryCreator.addCashValue(mSendReport.getSerial(), Calendar.getInstance().getTime());
                mImageHistory.setImageResource(R.drawable.ic_history_on);
                mOpenButton.setVisibility(View.VISIBLE);
                mOpenButton.setText(R.string.open_report);
                mOpenButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOpenButton.setVisibility(View.INVISIBLE);
                        openPdfIntent(mSendReport);
                    }
                });
                mQRScanner.setSuccessListener(initQRReport);
            }
            mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            getResultProcessingEvent(R.string.video_not_sending, null);
        }
    }

    private void openPdfIntent(VideoReport report){
        Intent intent = new Intent(this, PdfActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pdf_name", report.getPdfName());
        intent.putExtras(bundle);
        startActivity(intent);
        /*
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Uri uri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", report.getReportPath());

        intent.setDataAndType(uri, "application/pdf");
        startActivity(intent);
         */

    }

    private void openFeedBackIntent(){
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");

        File logs = new File(this.getFilesDir(),  "log.txt");
        try {
            FileWriter fileWriter = new FileWriter(logs);
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.write(mReportCreator.getCameraConfig() + mLogs.getAllLogs());
            bw.flush();
            bw.close();
        } catch (Exception e) {
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", logs);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "tli3.term@mail.ru" });
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.feedback));

        emailIntent.setPackage("com.google.android.gm");

        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    private void openInformationIntent(){
        Intent intent = new Intent(this, InformationActivity.class);
        startActivity(intent);
    }

    private void changeLanguage(){
        Resources resources = getResources();
        Locale locale = new Locale(resources.getString(R.string.change_lang));
        Locale.setDefault(locale);
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        finish();
        startActivity(getIntent());
    }

    private void openViewEvent(){
        mOpenButton.setVisibility(View.INVISIBLE);
        mProgressVideoSend.setVisibility(View.INVISIBLE);
        mProgressRecording.setVisibility(View.INVISIBLE);
        mInfoView.setVisibility(View.INVISIBLE);
        mSerialView.setVisibility(View.INVISIBLE);
    }

    private void checkPermissionEvent(){
        mOpenButton.setVisibility(View.VISIBLE);
        mOpenButton.setText(R.string.start);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission();
            }
        });

        mInfoView.setVisibility(View.VISIBLE);
        mInfoView.setText(R.string.permission);

        mImageFlash.setEnabled(false);
        mImageHistory.setEnabled(false);
    }

    private void connectWifiEvent(){
        mOpenButton.setVisibility(View.VISIBLE);
        mOpenButton.setText(R.string.start);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( isWifiConnection() ){ startCameraConnectionEvent(); }
            }
        });

        mInfoView.setVisibility(View.VISIBLE);
        mInfoView.setText(R.string.wifi_error);

        mImageFlash.setEnabled(false);
        mImageHistory.setEnabled(true);
    }

    private void startCameraConnectionEvent(){
        mOpenButton.setVisibility(View.INVISIBLE);
        mInfoView.setVisibility(View.VISIBLE);
        if(mCameraPreview.isAvailable()){
            connectCameraDevice();
            mLogs.addLog(LogCreator.REFRESH_CAMERA_ACTIVITY);
            return;
        }
        mCameraPreview.setSurfaceTextureListener(surfaceTextureListener);
        mLogs.addLog(LogCreator.START_CAMERA_ACTIVITY);
    }

    private void initDeviceEvent(){
        mInfoView.setVisibility(View.VISIBLE);
        mOpenButton.setVisibility(View.INVISIBLE);
        mProgressRecording.setVisibility(View.INVISIBLE);
        mSerialView.setVisibility(View.INVISIBLE);

        mImageHistory.setEnabled(true);
        mImageMenu.setEnabled(true);
        mImageFlash.setEnabled(true);

        mInfoView.setText(R.string.init_device);
        mImageQrView.setImageResource(R.drawable.ic_qr_img);
        mLogs.addLog(LogCreator.INIT_DEVICE_STATE);
    }

    private void checkExistDeviceEvent(String serial){
        mLogs.addLog(LogCreator.CHECK_DEVICE_EXIST_STATE, serial);
        mInfoView.setText(R.string.check_exist);
        mProgressVideoSend.setVisibility(View.VISIBLE);
    }

    private void refreshWifiConnection(){
        mInfoView.setText(R.string.wifi_error);
        mProgressVideoSend.setVisibility(View.INVISIBLE);
        mOpenButton.setVisibility(View.VISIBLE);
        mOpenButton.setText(R.string.start);
        mOpenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isWifiConnection()){
                    initDeviceEvent();
                    mQRScanner.setSuccessListener(initQRReport);
                    mQRScanner.startScanning(mCameraPreview.getBitmap(), mReportCreator.getRotationCamera());
                }
            }
        });
    }

    private void searchInitDeviceEvent(String serial){
        mLogs.addLog(LogCreator.INIT_DEVICE_SEARCH_STATE, serial);
        mProgressVideoSend.setVisibility(View.INVISIBLE);
        mProgressRecording.setVisibility(View.INVISIBLE);
        mSerialView.setVisibility(View.INVISIBLE);
        mImageHistory.setEnabled(true);
        mImageMenu.setEnabled(true);
        mInfoView.setText(R.string.search_device);
        mImageQrView.setImageResource(R.drawable.k);
    }

    private void startRecordReportEvent(){
        mLogs.addLog(LogCreator.START_RECORDING_REPORT_STATE);
        mSerialView.setVisibility(View.VISIBLE);
        mProgressRecording.setVisibility(View.VISIBLE);
        mImageHistory.setEnabled(false);
        mImageMenu.setEnabled(false);
        mProgressRecording.setProgress(0);
        mSerialView.setText(mVideoReport.getSerial10());
        mInfoView.setText(R.string.hold_button_device);
    }

    private void readyVideoReportEvent(){
        mLogs.addLog(LogCreator.READY_RECORDING_REPORT_STATE);
        mInfoView.setText(R.string.ready_report);
    }

    private void sendVideoReportEvent(){
        mLogs.addLog(LogCreator.SENDING_VIDEO_REPORT_STATE);
        mProgressRecording.setVisibility(View.INVISIBLE);
        mSerialView.setVisibility(View.INVISIBLE);
        mInfoView.setText(R.string.get_result);
        mImageQrView.setVisibility(View.INVISIBLE);
        mProgressVideoSend.setVisibility(View.VISIBLE);
    }

    private void getResultProcessingEvent(int result, String log){
        mInfoView.setText(result);
        mImageHistory.setEnabled(true);
        mImageMenu.setEnabled(true);
        if(result != ERROR_PROCESSING){ log = mInfoView.getText().toString(); }
        mLogs.addLog(LogCreator.PROCESSING_RESULT_STATE, log);
        mImageQrView.setImageResource(R.drawable.ic_qr_img);
        mImageQrView.setVisibility(View.VISIBLE);
        mProgressVideoSend.setVisibility(View.INVISIBLE);
    }

    private void errorHandler(String message){
        mLogs.addLog(message);
        mInfoView.setText(message);
        if(mQRScanner != null){ mQRScanner.disableScanner(); }
    }

}