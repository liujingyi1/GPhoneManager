package com.android.gphonemanager.view;

import com.android.gphonemanager.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;

public class SmoothProgressView extends View {
	
    private int mViewHeight;
    private int mViewWidth;
    
    private int mWidthOne;
    private int mWidthTwo;
    
    private float percentOne = 0.2f;
    private float percentTwo = 0.4f;
    private int one_color;
    private int two_color;
    private Paint mOnePaint;
    private Paint mTwoPaint;
    private int speed = 5;
    
    Context mContext;
    
    public SmoothProgressView(Context context) {
        this(context, null);
    }

    public SmoothProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public SmoothProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
    	super(context, attrs, defStyleAttr);
    	
    	mContext = context;
    	
    	one_color = getResources().getColor(R.color.percent_color_one);
    	two_color = getResources().getColor(R.color.percent_color_two);
    	
    	mOnePaint = new Paint();
    	mOnePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    	mOnePaint.setColor(one_color);
    	mOnePaint.setAntiAlias(true);
    	
    	mTwoPaint = new Paint();
    	mTwoPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    	mTwoPaint.setColor(two_color);
    	mTwoPaint.setAntiAlias(true);
	    mOnePaint.setStrokeWidth(1);
	    mTwoPaint.setStrokeWidth(1);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
	    mViewWidth = widthSize;
	    mViewHeight = heightSize;
	    

	    
	    setMeasuredDimension(mViewWidth, mViewHeight);
    }
    
    public void setPercent(float percentOnt, float PercentTwo) {
		this.percentOne = percentOnt;
		this.percentTwo = PercentTwo;
		
		invalidate();
	}
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	// TODO Auto-generated method stub
    	super.onLayout(changed, left, top, right, bottom);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	
		mWidthOne = (int)(mViewWidth * percentOne);
		mWidthTwo = (int)(mViewWidth * percentTwo);
		
    	RectF rectF1 = new RectF(0, 0, mWidthOne, mViewHeight);
    	RectF rectF2 = new RectF(mWidthOne, 0, mWidthOne+mWidthTwo, mViewHeight);
    	
    	canvas.drawRect(rectF1, mOnePaint);
    	canvas.drawRect(rectF2, mTwoPaint);
    }
    
}
