package com.android.gphonemanager.applock;

import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.TextView;

import com.android.gphonemanager.R;

public class FpmAuthenticationCallback extends FingerprintManager.AuthenticationCallback {
	private static final String TAG = "Gmanager.FpmAuthenticationCallback";
	
	public interface OnAuthenticationSucceeded {
		void onSucceeded();
	};
	
	boolean mSelfCancelled;
	private FingerprintManager fingerPrintManager;
	private CancellationSignal mCancellationSignal;
	private OnAuthenticationSucceeded onAuthenticationSucceeded;
	private TextView prompt;
	
	public FpmAuthenticationCallback(FingerprintManager mFingerPrintManager, TextView prompt,
			OnAuthenticationSucceeded onAuthenticationSucceeded) {
		this.fingerPrintManager = mFingerPrintManager;
		this.prompt = prompt;
		this.onAuthenticationSucceeded = onAuthenticationSucceeded;
	}
	
	public boolean isFingerprintAuthAvailable() {
        return fingerPrintManager.isHardwareDetected()
                && fingerPrintManager.hasEnrolledFingerprints();
    }
	
	public void startListening(FingerprintManager.CryptoObject cryptoObject) {
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        Log.v(TAG, "startListening");
        mCancellationSignal = new CancellationSignal();
        mSelfCancelled = false;
        prompt.setText("");
        fingerPrintManager
                .authenticate(cryptoObject, mCancellationSignal, 0 /* flags */, this, null);
    }
	
	public void stopListening() {
		Log.v(TAG, "stopListening");
        if (mCancellationSignal != null) {
            mSelfCancelled = true;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }
	
	@Override
	public void onAuthenticationError(int errorCode, CharSequence errString) {
		super.onAuthenticationError(errorCode, errString);
		Log.d(TAG, "onAuthenticationError:"+errString);
		prompt.setText(errString);
	}
	
	@Override
	public void onAuthenticationFailed() {
		super.onAuthenticationFailed();
		Log.d(TAG, "onAuthenticationFailed");
		prompt.setText(R.string.call_back_authentication_failed);
	}
	
	@Override
	public void onAuthenticationSucceeded(AuthenticationResult result) {
		super.onAuthenticationSucceeded(result);
		Log.d(TAG, "onAuthenticationSucceeded");
		if (onAuthenticationSucceeded != null) {
			onAuthenticationSucceeded.onSucceeded();
		}
		prompt.setText("");
	}
	
	@Override
	public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
		super.onAuthenticationHelp(helpCode, helpString);
		Log.d(TAG, "onAuthenticationHelp:"+helpString);
		prompt.setText(helpString);
	}
}
