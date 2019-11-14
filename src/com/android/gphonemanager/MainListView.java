package com.android.gphonemanager;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ListView;

public class MainListView extends ListView {
	private static final String TAG = "Gmanager.MainListView";
	private static final float Y_INVALID = -100000;
	
	public static final int HEADER_LENGTH_BIG = 500;
	
	public enum State {
		BIG, SMALL
	}
	
	public enum ScrollState {
		IDLE, UP, DOWN
	}
	
	OnTopChangedListener mOnTopChangedListener;
	boolean isTop = false;
	boolean isFirstVisible0 = false;
	float deltaY = 0;
	float topY = Y_INVALID;
	float preY = Y_INVALID;
	float buffer =  0;
	
	State mState = State.BIG;
	ScrollState mScrollState = ScrollState.IDLE;
	
	public void setIsTop(boolean isTop) {
		Log.d(TAG, "isTop="+isTop);
		this.isTop = isTop;
	}
	
	public void setIsFirstVisible0(boolean isFirstVisible0) {
		Log.d(TAG, "isFirstVisible0="+isFirstVisible0);
		this.isFirstVisible0 = isFirstVisible0;
	}
	
	public void setState(State state) {
		mState = state;
	}
	
	interface OnTopChangedListener {
		void changed(float delta);
	} 
	
	public void setOnTopChangedListener(OnTopChangedListener l) {
		mOnTopChangedListener = l;
	}
	
	public MainListView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public MainListView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MainListView(Context context) {
		this(context, null);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		int actionMasked = ev.getActionMasked();
		switch (actionMasked) {
		case MotionEvent.ACTION_DOWN: {
			topY = Y_INVALID;
			mScrollState = ScrollState.IDLE;
			
			float y = ev.getY();
			preY = y;
			if (isTop && topY == Y_INVALID) {
				topY = y;
			}
			if (mState == State.BIG) {
				deltaY = HEADER_LENGTH_BIG;
			} else {
				deltaY = 0;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			float y = ev.getY();
			if (preY != Y_INVALID) {
				if (y > preY) {
					mScrollState = ScrollState.DOWN;
				} else if (y < preY) {
					mScrollState = ScrollState.UP;
				}
			}
			
			switch (mScrollState) {
			case DOWN: {
				Log.d(TAG, "DOWN:");
				if (isTop) {
					if (topY == Y_INVALID) {
						topY = y;
					}
					Log.d(TAG, "buffer="+(y - preY));
					deltaY += y - preY;
					Log.d(TAG, "deltaY="+deltaY);
					if (deltaY <= HEADER_LENGTH_BIG) {
						if(mOnTopChangedListener != null) {
							mOnTopChangedListener.changed(deltaY);
						}
					} else if (deltaY > HEADER_LENGTH_BIG) {
						deltaY = HEADER_LENGTH_BIG;
						if(mOnTopChangedListener != null) {
							mOnTopChangedListener.changed(deltaY);
						}
					}
				}
				break;
			}
			case UP: {
				Log.d(TAG, "UP:");
				if (isFirstVisible0) {
					Log.d(TAG, "buffer="+(y - preY));
					deltaY += y - preY;
					Log.d(TAG, "deltaY="+deltaY);
					if (deltaY >= 0f) {
						if(mOnTopChangedListener != null) {
							mOnTopChangedListener.changed(deltaY);
							scrollListBy((int) (y - preY - 0.5f));
						}
					} else {
						deltaY = 0f;
						if(mOnTopChangedListener != null) {
							mOnTopChangedListener.changed(deltaY);
						}
					}
				}
				break;
			}

			default:
				break;
			}
			
			preY = y;
			break;
		}
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			Log.d(TAG, "ACTION_UP");
			if (isTop && deltaY > HEADER_LENGTH_BIG / 2) {
				deltaY = HEADER_LENGTH_BIG;
				mState = State.BIG;
			} else {
				deltaY = 0;
				mState = State.SMALL;
			}
			
			
			topY = Y_INVALID;
			preY = Y_INVALID;
			mScrollState = ScrollState.IDLE;
			if(mOnTopChangedListener != null) {
				mOnTopChangedListener.changed(deltaY);
			}
			deltaY = 0;
			break;
		default:
			break;
		}
		
		return super.onTouchEvent(ev);
	}
	
	@Override
	protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX,
			boolean clampedY) {
//		Log.e(TAG, "onOverScrolled: scrollX="+scrollX+", scrollY="+scrollY+", clampedX="+clampedX
//				+ ", clampedY="+clampedY);
		super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
	}
}
