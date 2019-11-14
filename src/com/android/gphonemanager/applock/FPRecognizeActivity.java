package com.android.gphonemanager.applock;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.gphonemanager.utils.Utils;

import com.android.gphonemanager.R;

public class FPRecognizeActivity extends Activity {
	private static final String TAG = "GPhoneManager.FPRecognizeActivity";
	
	private static final String TAG_FRAGMENT_PASSWORD = "password";
	private static final String TAG_FRAGMENT_PATTERN = "pattern";
	private static final String TAG_FRAGMENT_FP = "fingerprint";
	
	public static final String EXTRA_KEY_ITEM_ID = "main_item_id";
	
	
	KeyguardManager mKeyguardManager;
	FingerprintManager mFingerprintManager;
	FragmentManager mFragmentManager;
	
	LockPatternUtils mLockPatternUtils;
	FPLockPatternFragment mLockPatternFragment;
	FPLockPasswordFragment mPasswordFragment;
	FPLockFpFragment mFpFragment;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.fp_lock_choose_activity);
		
		mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
		
		if (!mKeyguardManager.isKeyguardSecure()) {
			// Show a message that the user hasn't set up a fingerprint or lock
			// screen.
            
            Intent i  = new Intent("android.credentials.UNLOCK");
			Toast.makeText(
					this,
					R.string.secure_not_set,
					Toast.LENGTH_LONG).show();
            startActivity(i);
            finish();
			return;
		}
		
		mLockPatternUtils = new LockPatternUtils(this);
		mLockPatternFragment = new FPLockPatternFragment();
		mPasswordFragment = new FPLockPasswordFragment();
		mFpFragment = new FPLockFpFragment();
		
		mFragmentManager = getFragmentManager();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	if (mFingerprintManager.hasEnrolledFingerprints()) {
			FragmentTransaction transaction = mFragmentManager.beginTransaction();
			transaction.replace(R.id.container, mFpFragment, TAG_FRAGMENT_FP);
        	transaction.commit();
		} else {
			launchLock();
		}
    }
    
    @Override
	public void onBackPressed() {
		String packageName = getIntent().getStringExtra("package");
		String className = getIntent().getStringExtra("class");
		
		if (null != packageName && !packageName.isEmpty()
				&& null != className && !className.isEmpty()) {
			//Eat it.
		} else {
			super.onBackPressed();
		}
	}
    
    public void launchLock() {
		int effectiveUserId = Utils.getEffectiveUserId(this);
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		//transaction.setCustomAnimations(R.anim.activity_open_enter, R.anim.activity_open_exit);
		switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(effectiveUserId)) {
        case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
        	Log.d(TAG, "LockPattern");
        	transaction.replace(R.id.container, mLockPatternFragment, TAG_FRAGMENT_PATTERN);
        	transaction.commit();
            break;
        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
        case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
        case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
        case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
        	transaction.replace(R.id.container, mPasswordFragment, TAG_FRAGMENT_PASSWORD);
        	transaction.commit();
        	Log.d(TAG, "LockPassword");
            break;
		}
	}
	
	public void unlock() {
		Intent intent = new Intent(FPRecognizeActivity.this, AppLockActivity.class);
		startActivity(intent);
		finish();
	}
}
