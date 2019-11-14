package com.android.gphonemanager.view;


import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RelativeLayout;

public class LockView extends RelativeLayout {

	private static final String TAG = "Gmanager.LockView";
	
	public interface OnBackKeyPressedListener {
		void pressed();
	}
	
	public void setOnBackKeyPressedListener(OnBackKeyPressedListener l) {
		mOnBackKeyPressedListener = l;
	}
	
	OnBackKeyPressedListener mOnBackKeyPressedListener;
	
	public LockView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public LockView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LockView(Context context) {
		this(context, null);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_BACK:
			Log.d(TAG, "KEYCODE_BACK");
			int action = event.getAction();
			if (action == KeyEvent.ACTION_DOWN) {
				if (mOnBackKeyPressedListener != null) {
					mOnBackKeyPressedListener.pressed();
					return true;
				}
			}
			break;
		default:
			break;
		}
		return super.dispatchKeyEvent(event);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}
	
	@Override
    public void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
}
