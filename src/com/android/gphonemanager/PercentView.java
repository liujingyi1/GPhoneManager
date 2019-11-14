package com.android.gphonemanager;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Created by chenlong.guo on 2016/11/17.
 */

public class PercentView extends View {

    private final static int HEIGHT = 200;
    private final static int WIDTH = 200;
    private final static int PADDING = 20;
    private static final int OUTER_COLOR = 0xffb6e3ff;
    private static final int INNER_BACKGROUND_COLOR = 0xff16a5cd;
    private static final int INNER_COLOR = 0xff2ff46f;
    private static final int TEXT_COLOR = 0xffffffff;

    private int mViewHeight;
    private int mViewWidth;
    private int mCenterX;
    private int mCenterY;
    private int mPaddingVertical;
    private int mPaddingHorizontal;
    private int mOuterRidus;
    private int mOuterWidth = 4;
    private int mInnerRidusDelta = 35;
    private int mInnerBackgroundWidth = 35;
    private int mInnerWidth = 12;
    private int mTextSize = 88;

    private Paint mOuterPaint;
    private Paint mInnerBackgroundPaint;
    private Paint mInnerPaint;
    private Paint mInnerBallPaint;
    private TextPaint mTextPaint;

    private float percent;

    public PercentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mOuterPaint = new Paint();
        mOuterPaint.setStyle(Paint.Style.STROKE);
        mOuterPaint.setStrokeWidth(mOuterWidth);
        mOuterPaint.setColor(OUTER_COLOR);
        mOuterPaint.setAntiAlias(true);

        mInnerBackgroundPaint = new Paint();
        mInnerBackgroundPaint.setStyle(Paint.Style.STROKE);
        mInnerBackgroundPaint.setStrokeWidth(mInnerBackgroundWidth);
        mInnerBackgroundPaint.setColor(INNER_BACKGROUND_COLOR);
        mInnerBackgroundPaint.setAntiAlias(true);

        mInnerPaint = new Paint();
        mInnerPaint.setStyle(Paint.Style.STROKE);
        mInnerPaint.setStrokeWidth(mInnerWidth * 2);
        mInnerPaint.setColor(INNER_COLOR);
        mInnerPaint.setAntiAlias(true);

        mInnerBallPaint = new Paint();
        mInnerBallPaint.setColor(INNER_COLOR);
        mInnerBallPaint.setAntiAlias(true);

        mTextPaint = new TextPaint();
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setAntiAlias(true);
    }

    public PercentView(Context context) {
        this(context, null);
    }

    public PercentView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public void setPercent(float percent) {
        this.percent = percent;
        postInvalidate();
    }


    public void setPercentWithAnim(float percent) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "percent", percent);
        objectAnimator.setDuration(1000);
        objectAnimator.setInterpolator(new OvershootInterpolator());
        objectAnimator.start();
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
        mCenterX = mViewWidth >> 1;
        mCenterY = mViewHeight >> 1;
        if (mViewWidth > mViewHeight) {
            mPaddingVertical = PADDING;
            mPaddingHorizontal = PADDING + ((mViewWidth - mViewHeight) >> 1);

            mOuterRidus = (mViewHeight >> 1) - PADDING;
        } else {
            mPaddingVertical = PADDING + ((mViewHeight - mViewWidth) >> 1);
            mPaddingHorizontal = PADDING;

            mOuterRidus = (mViewWidth >> 1) - PADDING;
        }

        setMeasuredDimension(mViewWidth, mViewHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawOuter(canvas);
        drawInnerBackground(canvas);
        drawInner(canvas);
        drawPercentText(canvas);
    }

    private void drawPercentText(Canvas canvas) {
        String percentString = (int)percent + "%";
        canvas.drawText(percentString, mCenterX, mCenterY + mTextSize / 4, mTextPaint);
    }

    private void drawInner(Canvas canvas) {
        float sweepAngle = percent / 100 * 250;
        RectF rectF = new RectF(mPaddingHorizontal + mInnerRidusDelta, mPaddingVertical + mInnerRidusDelta ,
                mViewWidth - mPaddingHorizontal - mInnerRidusDelta, mViewHeight - mPaddingVertical - mInnerRidusDelta);
        canvas.drawArc(rectF, -215, sweepAngle, false, mInnerPaint);
        double angle = -215 * Math.PI / 180;
        int l = mOuterRidus - mInnerRidusDelta;
        double xDelta = Math.cos(angle) * l;
        double yDelta = Math.sin(angle) * l;
        canvas.drawCircle(mCenterX + (float)xDelta, mCenterY + (float)yDelta, mInnerWidth, mInnerBallPaint);
        angle = (-215 + sweepAngle) * Math.PI / 180;
        xDelta = Math.cos(angle) * l;
        yDelta = Math.sin(angle) * l;
        canvas.drawCircle(mCenterX + (float)xDelta, mCenterY + (float)yDelta, mInnerWidth, mInnerBallPaint);
    }

    private void drawInnerBackground(Canvas canvas) {
        mInnerBackgroundPaint.setStrokeWidth(mInnerBackgroundWidth);

        RectF rectF = new RectF(mPaddingHorizontal + mInnerRidusDelta, mPaddingVertical + mInnerRidusDelta ,
                mViewWidth - mPaddingHorizontal - mInnerRidusDelta, mViewHeight - mPaddingVertical - mInnerRidusDelta);
        canvas.drawArc(rectF, 40, -260, false, mInnerBackgroundPaint);


        double angle = 40 * Math.PI / 180;
        double xDelta = Math.cos(angle) * (mOuterRidus - mInnerRidusDelta + (mInnerBackgroundWidth >> 1));
        double yDelta = Math.sin(angle) * (mOuterRidus - mInnerRidusDelta);
        mInnerBackgroundPaint.setStrokeWidth((float) (mInnerBackgroundWidth * Math.cos(angle)));
        /*canvas.drawLine(mCenterX - (float)(2 + xDelta + mInnerBackgroundWidth * Math.sin(angle) / 2), mCenterY + (float)yDelta,
                mCenterX + (float)(2 + xDelta +  mInnerBackgroundWidth * Math.sin(angle) / 2), mCenterY + (float)yDelta, mInnerBackgroundPaint);*/
        canvas.drawLine(mCenterX - (float)(xDelta), mCenterY + (float)yDelta,
                mCenterX + (float)(xDelta), mCenterY + (float)yDelta, mInnerBackgroundPaint);
    }

    private void drawOuter(Canvas canvas) {
        RectF rectF = new RectF(mPaddingHorizontal, mPaddingVertical, mViewWidth - mPaddingHorizontal, mViewHeight - mPaddingVertical);
        canvas.drawArc(rectF, 40, -260, false, mOuterPaint);
        double angle = 40 * Math.PI / 180;
        double startXDelta = Math.cos(angle) * (mOuterRidus - (mOuterWidth >> 1));
        double startYDelta = Math.sin(angle) * (mOuterRidus - (mOuterWidth >> 1));
        double endXDelta = Math.cos(angle) * (mOuterRidus + 20);
        double endYDelta = Math.sin(angle) * (mOuterRidus + 20);

        canvas.drawLine(mCenterX + (float)startXDelta, mCenterY + (float)startYDelta,
                mCenterX + (float)endXDelta, mCenterY + (float)endYDelta, mOuterPaint);

        canvas.drawLine(mCenterX - (float)startXDelta, mCenterY + (float)startYDelta,
                mCenterX - (float)endXDelta, mCenterY + (float)endYDelta, mOuterPaint);
    }

}
