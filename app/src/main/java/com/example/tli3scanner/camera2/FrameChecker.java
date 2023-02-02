package com.example.tli3scanner.camera2;

import android.util.Range;

public class FrameChecker {

    private final int SKIP_FRAME = 50;
    private final int MAX_FOCUS_ERROR = 50;

    private int mSkipFrame = SKIP_FRAME;
    private boolean mEnableAFMode;
    private int mCountError;

    private Range<Long> mCorrectFrameRange;
    private Range<Float> mCorrectFocusRange;

    FrameChecker(boolean af, float diop, long frame){
        mEnableAFMode = af;
        mCorrectFrameRange = new Range<>(frame - 500, frame + 500);
        mCorrectFocusRange = new Range<>(diop - 0.5f,diop + 0.5f);
    }

    public boolean isErrorCapture(long duration, float focus, int fstate){
        if(mSkipFrame != 0){
            mSkipFrame--;
            return false;
        }
        if(!isCorrectFocusDistance(focus, fstate) || !isCorrectFrameDuration(duration)){
            resetChecker();
            return true;
        }
        return false;
    }

    public void setAFMode(boolean af){
        mEnableAFMode = af;
    }

    private void resetChecker(){
        mSkipFrame = SKIP_FRAME;
    }

    private boolean isCorrectFrameDuration(long duration){
        return mCorrectFrameRange.contains(duration);
    }

    private boolean isCorrectFocusDistance(float focus, int fstate){
        if(mEnableAFMode && !mCorrectFocusRange.contains(focus) || !mEnableAFMode && fstate == 0){
            mCountError++;
            if(mCountError != MAX_FOCUS_ERROR){ return true; }
            mCountError = 0;
            return false;
        }
        mCountError = 0;
        return true;
    }
}
