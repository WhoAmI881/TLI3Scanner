package com.example.tli3scanner;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class QRCodeScanner {

    private final float RATIO = 3;
    private final int STEP = 50;
    private final long TIME = 0;

    private ImageView mViewQR;
    private Size mSizeQR;
    private Handler mHandler;
    private long mDelay;
    private boolean mEnable;
    private boolean mMaxSize;


    private BarcodeScanner mScanner;
    private OnSuccessListener<List<Barcode>> mSuccessListener;
    private OnFailureListener mFailureListener;

    public QRCodeScanner(ImageView qrView, Handler handler){
        mViewQR = qrView;
        mHandler = handler;
        mSizeQR = new Size(mViewQR.getLayoutParams().width, mViewQR.getLayoutParams().height);
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        mScanner = BarcodeScanning.getClient(options);

        mDelay = TIME;
    }

    public void setDelay(long delay){
        mDelay = delay;
    }

    public void startScanning(Bitmap bitmap, int rotation){
        mHandler.removeCallbacksAndMessages(null);
        if(bitmap == null || !mEnable){
            resetSizeViewQR();
            return;
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanner.process(InputImage.fromBitmap(getAnalyseBitmap(bitmap), rotation))
                        .addOnSuccessListener(mSuccessListener).addOnFailureListener(mFailureListener);
            }
        }, mDelay);

        mDelay = TIME;
    }

    public void enableScanner(){
        mEnable = true;
    }

    public void disableScanner(){
        mEnable = false;
    }

    private Bitmap getAnalyseBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = mViewQR.getWidth();
        int height = mViewQR.getHeight();

        int[] pixels = new int[width * height];

        int start_y = (bitmap.getHeight() - height) / 2;
        int start_x = (bitmap.getWidth() - width) / 2;

        bitmap.getPixels(pixels, 0, width, start_x, start_y, width, height);
        bitmap.setHeight(height);
        bitmap.setWidth(width);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

    public void setSuccessListener(OnSuccessListener<List<Barcode>> event){
        mSuccessListener = event;
    }

    public void setFailureListener(OnFailureListener event){
        mFailureListener = event;
    }

    public String getSerialFromBarcode(List<Barcode> barcodes, boolean size){
        if(!barcodes.isEmpty()){
            Barcode barcode = barcodes.get(0);
            if(!size || checkSizeQR(barcode.getBoundingBox())){
                //refreshSizeViewQR(barcode.getCornerPoints());
                if(!mMaxSize) maximiseRect();
                String Url = barcode.getRawValue();
                return Url.substring(Url.length() - 4);
            }
        }
        resetSizeViewQR();
        return null;
    }

    public void resetSizeViewQR(){
        ViewGroup.LayoutParams params = mViewQR.getLayoutParams();
        if(params.height == mSizeQR.getHeight() && params.width == mSizeQR.getWidth()){
            return;
        }
        mMaxSize = false;
        params.height = mSizeQR.getHeight();
        params.width = mSizeQR.getWidth();
        mViewQR.setLayoutParams(params);
        mViewQR.setVisibility(View.VISIBLE);
    }

    private void maximiseRect(){
        ViewGroup.LayoutParams params = mViewQR.getLayoutParams();

        params.height = params.height * 2;
        params.width = params.width * 2;

        mViewQR.setLayoutParams(params);
        mViewQR.setVisibility(View.INVISIBLE);
        mMaxSize = true;
    }

    private void refreshSizeViewQR(Point[] points){
        ViewGroup.LayoutParams params = mViewQR.getLayoutParams();
        int height = params.height;
        int width = params.width;

        int X = (points[2].x + points[0].x - height)/2;
        int Y = (points[2].y + points[0].y - width)/2;

        int Len = (int)Math.sqrt(X*X+Y*Y) + STEP;

        height = mSizeQR.getHeight() + Len;
        width = mSizeQR.getWidth() + Len;

        params.height = height;
        params.width = width;

        mViewQR.setLayoutParams(params);
    }

    private boolean checkSizeQR(Rect qr){
        return qr.width() * qr.height() > mSizeQR.getWidth() * mSizeQR.getHeight()/RATIO;
    }

}
