package com.android.gphonemanager.view;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class ActivityLoadingView extends View {

	private final static int CIRCLE_COUNT = 5;
	private final static int MOVE_RADIUS = 160;
	private final static int DISMISS_RADIUS = 400;
	private final static int MOVE_RADIUS_BUFFER = 20;
	private final static int CIRCLE_RADIUS = 20;
	
	private final static int HEIGHT = 200;
	private final static int WIDTH = 200;
	private final static double DELTA_BUFFER = Math.PI / 10;
	
	private final static int MSG_MOVE = 101;
	private final static int MSG_DISMISS = 102;

	private Paint mPaint;
	private Paint mBackgroundPaint;
	private int mHeight;
	private int mWidth;
	private int viewCenterX;
	private int viewCenterY;
	private double mDelta = 0.0d;
	private double mMoveRadius = MOVE_RADIUS;
	
	private int mBackgroundStrokeWidth;
	private int mBackgroundRadius;
	
	private ArrayList<CircleInfo> mCircleInfos = new ArrayList<>();
	int[] colors = new int[] { 0xffffb5c5, 0xffd15fee, 0xffadff2f, 0xff87ceff,
			0xff00ee00, };
	
	boolean dismissed = false;

	private Handler H = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_DISMISS:
				mMoveRadius += MOVE_RADIUS_BUFFER;
				mBackgroundRadius += MOVE_RADIUS_BUFFER;
				if (mMoveRadius >= DISMISS_RADIUS) {
					setVisibility(View.GONE);
					break;
				}
			case MSG_MOVE:
				mDelta += DELTA_BUFFER;
				for (int count = 0; count < CIRCLE_COUNT; count++) {
					double angle = getAngle(count, mDelta);
					mCircleInfos.get(count).setCenter((int) (viewCenterX + mMoveRadius * Math.sin(angle)), 
							(int) (viewCenterY + mMoveRadius * Math.cos(angle)));
				}
				postInvalidate();
				break;

			default:
				break;
			}
		};
	};
	
	public ActivityLoadingView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mBackgroundPaint = new Paint();
		mBackgroundPaint.setColor(0xffffffff);
		mBackgroundPaint.setStyle(Paint.Style.STROKE);
		mBackgroundPaint.setAntiAlias(true);
	}

	public ActivityLoadingView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ActivityLoadingView(Context context) {
		this(context, null);
	}

	private void initCircle(int viewWidth, int viewHeight) {
		mCircleInfos.clear();
		viewCenterX = viewWidth >> 1;
		viewCenterY = viewHeight >> 1;
		for (int count = 0; count < CIRCLE_COUNT; count++) {
			double angle = getAngle(count, 0);
			CircleInfo circleInfo = new CircleInfo(
					(int) (viewCenterX + MOVE_RADIUS * Math.sin(angle)),
					(int) (viewCenterY + MOVE_RADIUS * Math.cos(angle)), CIRCLE_RADIUS, colors[count]);
			mCircleInfos.add(circleInfo);
		}
		mBackgroundStrokeWidth = (int)((Math.sqrt(viewWidth * viewWidth + viewHeight * viewHeight) / 2) + 0.5f);
		mBackgroundRadius = mBackgroundStrokeWidth >> 1;
		mBackgroundPaint.setStrokeWidth(mBackgroundStrokeWidth);
	}
	
	private double getAngle(int index, double delta) {
		return Math.PI * 2 * index / CIRCLE_COUNT + delta;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		if (MeasureSpec.EXACTLY == widthMode) {
			mWidth = widthSize;
		} else {
			mWidth = WIDTH;
		}

		if (MeasureSpec.EXACTLY == heightMode) {
			mHeight = heightSize;
		} else {
			mHeight = HEIGHT;
		}

		setMeasuredDimension(mWidth, mHeight);

		initCircle(mWidth, mHeight);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawCircle(viewCenterX, viewCenterY, mBackgroundRadius, mBackgroundPaint);
		if (dismissed) {
			if (mMoveRadius < DISMISS_RADIUS) {
				for (CircleInfo circleInfo:mCircleInfos) {
					mPaint.setColor(circleInfo.color);
					canvas.drawCircle(circleInfo.centerX, circleInfo.centerY, circleInfo.radius, mPaint);
				}
				H.sendEmptyMessageDelayed(MSG_DISMISS, 50);
			}
		} else {
			for (CircleInfo circleInfo:mCircleInfos) {
				mPaint.setColor(circleInfo.color);
				canvas.drawCircle(circleInfo.centerX, circleInfo.centerY, circleInfo.radius, mPaint);
			}
			H.sendEmptyMessageDelayed(MSG_MOVE, 50);
		}
	}
	
	public void dismiss() {
		dismissed = true;
	}

	class CircleInfo {

		public CircleInfo(int centerX, int centerY, int radius, int color) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.radius = radius;
			this.color = color;
		}

		public void setCenter(int centerX, int centerY) {
			this.centerX = centerX;
			this.centerY = centerY;
		}

		int centerX;
		int centerY;
		int radius;
		int color;
	}

}
