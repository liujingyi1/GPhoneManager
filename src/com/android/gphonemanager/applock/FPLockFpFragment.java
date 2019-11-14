package com.android.gphonemanager.applock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.gphonemanager.R;

public class FPLockFpFragment extends Fragment {
	private static final String TAG = "GPhoneManager.FPLockFpFragment";
	
	private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;
	private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;
	 
	TextView textPrompt;
	TextView switchView;
	View iconView;
	
	FingerprintManager mFingerprintManager;
	MyCallBack myCallBack;
	private int mIconTouchCount;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fp_lock_fp_fragment, null);
		textPrompt = (TextView)view.findViewById(R.id.ptext);
		switchView = (TextView)view.findViewById(R.id.btn_switch);
		switchView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (getActivity() instanceof FPRecognizeActivity) {
	        		((FPRecognizeActivity)getActivity()).launchLock();
	        	}
			}
		});
		myCallBack = new MyCallBack(mFingerprintManager, textPrompt, getActivity());
		
		iconView = view.findViewById(R.id.icon);
		iconView.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getActionMasked();
				switch (action) {
				case MotionEvent.ACTION_DOWN:
					mIconTouchCount++;
                    if (mIconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                    	showIconTouchDialog();
                    } else {
                    	iconView.postDelayed(mShowDialogRunnable, ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN);
                    }
					break;
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_UP:
					iconView.removeCallbacks(mShowDialogRunnable);
					break;
				}
				return true;
			}
		});
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (myCallBack != null) {
			myCallBack.startListening(null);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (myCallBack != null) {
			myCallBack.stopListening();
		}
	}
	
	private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showIconTouchDialog();
        }
    };
    
    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        new IconTouchDialog().show(getFragmentManager(), null /* tag */);
    }
    
    public static class IconTouchDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.fingerprint_touch_dialog_title)
                    .setMessage(R.string.fingerprint_touch_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
            return builder.create();
        }
    }
	
	class MyCallBack extends FingerprintManager.AuthenticationCallback {
		boolean mSelfCancelled;
		private FingerprintManager fingerPrintManager;
		private CancellationSignal mCancellationSignal;
		TextView prompt;
		
		Activity mActivity;
		
		public MyCallBack(FingerprintManager mFingerPrintManager, TextView prompt, Activity mActivity) {
			this.fingerPrintManager = mFingerPrintManager;
			this.prompt = prompt;
			this.mActivity = mActivity;
		}
		
		public boolean isFingerprintAuthAvailable() {
	        return fingerPrintManager.isHardwareDetected()
	                && fingerPrintManager.hasEnrolledFingerprints();
	    }
		
		public void startListening(FingerprintManager.CryptoObject cryptoObject) {
	        if (!isFingerprintAuthAvailable()) {
	            return;
	        }
	        mCancellationSignal = new CancellationSignal();
	        mSelfCancelled = false;
	        prompt.setText("");
	        fingerPrintManager
	                .authenticate(cryptoObject, mCancellationSignal, 0 /* flags */, this, null);
	    }
		
		public void stopListening() {
	        if (mCancellationSignal != null) {
	            mSelfCancelled = true;
	            mCancellationSignal.cancel();
	            mCancellationSignal = null;
	        }
	    }
		
		@Override
		public void onAuthenticationError(int errorCode, CharSequence errString) {
			super.onAuthenticationError(errorCode, errString);
			Log.d(TAG, "onAuthenticationError");
			prompt.setText("" + errString);
		}
		
		@Override
		public void onAuthenticationFailed() {
			super.onAuthenticationFailed();
			Log.d(TAG, "onAuthenticationFailed");
			prompt.setText("onAuthenticationFailed");
		}
		
		@Override
		public void onAuthenticationSucceeded(AuthenticationResult result) {
			super.onAuthenticationSucceeded(result);
			Log.d(TAG, "onAuthenticationSucceeded");
			prompt.setText("");
			if (getActivity() instanceof FPRecognizeActivity) {
        		((FPRecognizeActivity)getActivity()).unlock();
        	}
		}
		
		@Override
		public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
			super.onAuthenticationHelp(helpCode, helpString);
			prompt.setText("" + helpString);
		}
	}
}
