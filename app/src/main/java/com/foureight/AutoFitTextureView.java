package com.foureight;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class AutoFitTextureView extends TextureView {
    private static final String TAG = "AutoFitTextureView";
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context){
        this(context,null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs){
        this(context,attrs,0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        double ratio = (double) mRatioWidth/mRatioHeight;
        double cRatio = (double) width/width;
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            Log.d("Camera2BasicFragment", "onMeasure: "+width+"//"+height);
            setMeasuredDimension(width, height);
        }else{
            if (ratio > 1) {
                //setMeasuredDimension( width * mRatioHeight / mRatioWidth , width);
                if (width < height * mRatioWidth / mRatioHeight) {
                    Log.d("Camera2BasicFragment", "onMeasure: " + width +"//"+(width * mRatioHeight / mRatioWidth));
                    setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
                } else {
                    Log.d("Camera2BasicFragment", "onMeasure: " + (height * mRatioWidth / mRatioHeight) +"//"+height);
                    setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
                }
            } else {
                setMeasuredDimension(width, height);
            }
        }
    }
}
