package com.android.gphonemanager.view;

import java.util.concurrent.CountDownLatch;

import com.android.gphonemanager.R;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.text.TextPaint;
import android.text.style.TtsSpan.ElectronicBuilder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.animation.LinearInterpolator;

import android.content.res.TypedArray;

public class CircleProgressView extends View {
	
    private final static int HEIGHT = 200;
    private final static int WIDTH = 200;
    
    private int blockHeightOut = 14;
    private int blockHeightin = 10;
    private int blockWidth = 3;
    private final int blockCount = 90;
    private int lineGap = 12;
    private int circleLineWidth = 5;
    private final int sweepAngle = 360;
    private final int startAngleOut = 90;
    private final int startAngleIn = 0;
    
    private int mainTextSize = 80;
    private int infoTextSize = 16;
    
    
    private int mCenterX;
    private int mCenterY;
    private int mViewHeight;
    private int mViewWidth;
    private int mOuterRadius;
//    private int mPaddingVertical;
//    private int mPaddingHorizontal;
//    private int mcurrentRadius;
//    private int mSquareWidth;
    
    private int mCurrentAngleOut;
    private int mCurrentAngleIn;
    
    private Paint mArcPaint;
    private Paint mBlockPaint;
    private Paint mTextPaint;
    
    private int percent;
    private String mInfo = "";
    
    private boolean start = false;
	private final static int MSG_MOVE = 101;
	private final static int MSG_DISMISS = 102;
    
	public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
	    blockHeightOut = getResources().getDimensionPixelOffset(R.dimen.circle_progress_block_height_out);
	    blockHeightin = getResources().getDimensionPixelOffset(R.dimen.circle_progress_block_height_in);
	    blockWidth = getResources().getDimensionPixelOffset(R.dimen.circle_progress_block_width);
	    lineGap = getResources().getDimensionPixelOffset(R.dimen.circle_progress_line_gap);
	    circleLineWidth = getResources().getDimensionPixelOffset(R.dimen.circle_progress_line_width);
	    mainTextSize = getResources().getDimensionPixelOffset(R.dimen.circle_progress_text_size);
	    infoTextSize = getResources().getDimensionPixelOffset(R.dimen.circle_progress_text_size_info);
		
		mArcPaint = new Paint();
		mArcPaint.setStyle(Paint.Style.STROKE);
		mArcPaint.setStrokeWidth(circleLineWidth);
		mArcPaint.setColor(Color.WHITE);
		mArcPaint.setAntiAlias(true);
		
		mBlockPaint = new Paint();
		mBlockPaint.setStyle(Paint.Style.STROKE);
		mBlockPaint.setStrokeWidth(blockWidth);
		mBlockPaint.setColor(Color.WHITE);
		mBlockPaint.setAntiAlias(true);
		
        mTextPaint = new TextPaint();
        mTextPaint.setStyle(Paint.Style.STROKE);
        int color = getResources().getColor(R.color.main_percent_color);
        mTextPaint.setColor(color);
        
        mTextPaint.setTextSize(mainTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setAntiAlias(true);
        
        mCurrentAngleOut = startAngleOut;
        mCurrentAngleIn = startAngleIn;
        
//        new Thread(animThread).start();
        
	}

    public CircleProgressView(Context context) {
        this(context, null);
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public void setPercent(int percent) {
        this.percent = percent;
        
        mCurrentAngleOut = startAngleIn + (int)(360 * ((float)percent / 100));
        
        if (!start) {
        	postInvalidate();
        }
    }
    
    public void setInfo(String info) {
        this.mInfo = info;
        if (!start) {
        	postInvalidate();
        }
    }
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        if (MeasureSpec.EXACTLY == widthMode) {
            mViewWidth = widthSize;
        } else {
            mViewWidth = WIDTH;
        }

        if (MeasureSpec.EXACTLY == heightMode) {
            mViewHeight = heightSize;
        } else {
            mViewHeight = HEIGHT;
        }
        mCenterX = (getPaddingLeft() + (mViewWidth - getPaddingRight())) / 2;//  mViewWidth >> 1;
        mCenterY = (getPaddingTop() + (mViewHeight - getPaddingBottom())) / 2;
        //mCenterY = mViewHeight >> 1;
        
        int width = mViewWidth - getPaddingLeft() - getPaddingRight();
        int height = mViewHeight - getPaddingBottom() - getPaddingTop();
        mOuterRadius = (Math.min(width, height)) / 2;
//        mcurrentRadius = mOuterRadius;

        setMeasuredDimension(mViewWidth, mViewHeight);
	}
	
	public void setStartAnim() {
		
		start  = true;
		startAnim();

	}
	
	public void stop() {
		start = false;
		line2stop = false;
		line1stop = false;

		//invalidate();
	}
	
	private void drawLine(Canvas canvas) {
		canvas.save();
		
		float angle = (float) sweepAngle / blockCount;
		canvas.rotate(mCurrentAngleOut);
		
		int radius = mOuterRadius;
		
		for (int i = 0; i < blockCount; i++) {
			mBlockPaint.setAlpha(200-(int)(190*( (float)i/blockCount) ));
			canvas.drawLine(0, -radius, 
					0, -radius+blockHeightOut, mBlockPaint);
			canvas.rotate(angle);
		}
		
		canvas.restore();
	}
	
	private void drawLine2(Canvas canvas) {
		canvas.save();
		
		float angle = (float) sweepAngle / blockCount;
		angle = -angle;
		canvas.rotate(mCurrentAngleIn);
		
		int radius = mOuterRadius - blockHeightOut - lineGap;
		
		for (int i = 0; i < blockCount; i++) {
			mBlockPaint.setAlpha(255-(int)(255*( (float)i/blockCount) ));
			canvas.drawLine(0, -radius, 
					0, -radius+blockHeightin, mBlockPaint);
			canvas.rotate(angle);
		}
		
		canvas.restore();
	}
	
	private void drawCircle(Canvas canvas) {
		int radius = mOuterRadius - blockHeightOut - blockHeightin - lineGap*2;
		canvas.drawCircle(0, 0, radius, mArcPaint);
	}
	
	private void drawText(Canvas canvas) {
        String percentString = percent + "%";
        mTextPaint.setTextSize(mainTextSize);
        canvas.drawText(percentString, 0, 0 + mainTextSize / 4, mTextPaint);
        
        mTextPaint.setTextSize(infoTextSize);
        canvas.drawText(mInfo, 0, mainTextSize /2 + infoTextSize, mTextPaint);
	}
	
	
	int sudu = 1;
	int count = 0;
	boolean preparestop = false;
	boolean line1stop = true;
	boolean line2stop = true;
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		//super.onDraw(canvas);

		canvas.translate(mCenterX, mCenterY);
		
//		int angle = startAngleIn + (int)(360 * ((float)percent / 100));
			
//		if (start) {
//		    mCurrentAngleOut -= (sweepAngle / blockCount) * 3;
//	    } else if (preparestop && ((Math.abs((mCurrentAngleOut % 360)) >= (angle + (sweepAngle / blockCount) * 3)) ||
//	    		(Math.abs((mCurrentAngleOut % 360)) <= (angle - (sweepAngle / blockCount) * 3)))) {
//	    	mCurrentAngleOut -= (sweepAngle / blockCount) * 3;
//	    } else {
//	    	mCurrentAngleOut = angle;
//	    	line1stop = true;
//	    }
		drawLine(canvas);
	
//		if (start) {
//		mCurrentAngleIn += (sweepAngle / blockCount) * 2;
//		} else if (preparestop && Math.abs((mCurrentAngleIn % 360)) != 0) {
//			mCurrentAngleIn += (sweepAngle / blockCount) * 2;
//		} else {
//			line2stop = true;
//		}
		drawLine2(canvas);
		
		if (line2stop && line1stop) {
			preparestop = false;
		}

		
	    drawCircle(canvas);
	    drawText(canvas);
		
		
//		try {
//			Thread.sleep(30);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		if (start) {
//		    invalidate();
//		}
		
	}

	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.draw(canvas);
	}
	
	private final Runnable animThread = new Runnable() {
		
		@Override
		public void run() {
			int i = 0;
			while (true) {
				
				if (start || preparestop) {
                    postInvalidate();
                    try {
						Thread.sleep(25);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
                    try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
			
		}
	};
	
    private void startAnim() {
    	
        ValueAnimator animator = ValueAnimator.ofInt(blockCount, 0);
        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				
				int pro = (int) animation.getAnimatedValue();
				int item = sweepAngle / blockCount;
				
				int angle = startAngleIn + (int)(blockCount * ((float)percent / 100));
				
				mCurrentAngleOut = item * pro;
				postInvalidate();
				
				if (!start && ((pro > angle-3) && (pro < angle+3))) {
					animator.cancel();
				}
			}
		});
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.start();
        
        
        ValueAnimator animatorin = ValueAnimator.ofInt(0, blockCount);
        animatorin.setDuration(800);
        animatorin.setInterpolator(new LinearInterpolator());
        animatorin.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				
				int pro = (int) animation.getAnimatedValue();
				int item = sweepAngle / blockCount;
				mCurrentAngleIn = item * pro;
				
				postInvalidate();
				
				if (!start && (pro > blockCount - 3 || pro < 3)) {
					mCurrentAngleIn = 0;
					animatorin.cancel();
				}
			}
		});
        animatorin.setRepeatCount(ValueAnimator.INFINITE);
        animatorin.setRepeatMode(ValueAnimator.RESTART);
        animatorin.start();
    }
	
	private class latticeCicle extends View{

		public latticeCicle(Context context) {
			super(context);
			
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			// TODO Auto-generated method stub
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
		
		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			super.onDraw(canvas);
		}
		
		
	}
	
}
