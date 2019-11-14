package com.android.gphonemanager.applock;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.gphonemanager.R;
import com.android.gphonemanager.utils.Utils;

public class FPLockPasswordFragment extends Fragment implements OnEditorActionListener {
	private static final String TAG = "GPhoneManager.FPLockPasswordFragment";
	
	private static final String KEY_NUM_WRONG_CONFIRM_ATTEMPTS
    		= "confirm_lock_password_fragment.key_num_wrong_confirm_attempts";
	
	private static final long ERROR_MESSAGE_TIMEOUT = 3000;
	
	private TextView mPasswordEntry;
	private TextViewInputDisabler mPasswordEntryInputDisabler;
	
	private TextView mHeaderTextView;
    private TextView mDetailsTextView;
    private TextView mErrorTextView;
	
	private LockPatternUtils mLockPatternUtils;
	private Handler mHandler = new Handler();
	private InputMethodManager mImm;
	private AsyncTask<?, ?, ?> mPendingLockCheck;
	
	private int mEffectiveUserId;
	private int mNumWrongConfirmAttempts;
	private boolean mIsAlpha;
	private CountDownTimer mCountdownTimer;
	private boolean mBlockImm;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mLockPatternUtils = new LockPatternUtils(getActivity());
        mEffectiveUserId = Utils.getEffectiveUserId(getActivity());

        if (savedInstanceState != null) {
            mNumWrongConfirmAttempts = savedInstanceState.getInt(
                    KEY_NUM_WRONG_CONFIRM_ATTEMPTS, 0);
        }
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                mEffectiveUserId);
		
		View view  = inflater.inflate(R.layout.fp_lock_password_fragment, null);
		
		mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener(this);
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);
        
        mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
        mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
        mErrorTextView = (TextView) view.findViewById(R.id.errorText);
        
        mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;
        
        mImm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        
        mHeaderTextView.setText(getDefaultHeader());
        mDetailsTextView.setText(getDefaultDetails());
        
        int currentType = mPasswordEntry.getInputType();
        mPasswordEntry.setInputType(mIsAlpha ? currentType
                : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
        
		return view;
		
	}
	
	private int getDefaultHeader() {
        return mIsAlpha ? R.string.lockpassword_confirm_your_password_header
                : R.string.lockpassword_confirm_your_pin_header;
    }
	
	private int getDefaultDetails() {
        return mIsAlpha ? R.string.lockpassword_confirm_your_password_generic
                : R.string.lockpassword_confirm_your_pin_generic;
    }

    private int getErrorMessage() {
        return mIsAlpha ? R.string.lockpassword_invalid_password
                : R.string.lockpassword_invalid_pin;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            resetState();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
            mPendingLockCheck = null;
        }
    }
    
    private boolean shouldAutoShowSoftKeyboard() {
        return mPasswordEntry.isEnabled();
    }
    
    private void resetState() {
        if (mBlockImm) return;
        mPasswordEntry.setEnabled(true);
        mPasswordEntryInputDisabler.setInputEnabled(true);
        if (shouldAutoShowSoftKeyboard()) {
            mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        long elapsedRealtime = SystemClock.elapsedRealtime();
        mPasswordEntry.setEnabled(false);
        mCountdownTimer = new CountDownTimer(
                elapsedRealtimeDeadline - elapsedRealtime,
                LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

            @Override
            public void onTick(long millisUntilFinished) {
                ///M: CR ALPS01808515. Avoid fragment did not attach to activity.
                if (!isAdded()) return;
                final int secondsCountdown = (int) (millisUntilFinished / 1000);
                showError(getString(
                        R.string.lockpattern_too_many_failed_confirmation_attempts,
                        secondsCountdown), 0);
            }

            @Override
            public void onFinish() {
                resetState();
                mErrorTextView.setText("");
                mNumWrongConfirmAttempts = 0;
            }
        }.start();
    }
    
    private void handleNext() {
        mPasswordEntryInputDisabler.setInputEnabled(false);
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
        }

        final String pin = mPasswordEntry.getText().toString();
        final boolean verifyChallenge = getActivity().getIntent().getBooleanExtra(
                "EXTRA_KEY_HAS_CHALLENGE", false);
        Intent intent = new Intent();
        if (verifyChallenge)  {
            if (isInternalActivity()) {
                startVerifyPassword(pin, intent);
                return;
            }
        } else {
            startCheckPassword(pin, intent);
            return;
        }

        onPasswordChecked(false, intent, 0, mEffectiveUserId);
    }
    
    private boolean isInternalActivity() {
        return false; //getActivity() instanceof ConfirmLockPassword.InternalActivity;
    }
    
    private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs,
            int effectiveUserId) {
        mPasswordEntryInputDisabler.setInputEnabled(true);
        if (matched) {
            //startDisappearAnimation(intent);
        	if (getActivity() instanceof FPRecognizeActivity) {
        		((FPRecognizeActivity)getActivity()).unlock();
        	}
        } else {
            if (timeoutMs > 0) {
                long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                        effectiveUserId, timeoutMs);
                handleAttemptLockout(deadline);
            } else {
                showError(getErrorMessage());
            }
        }
    }
    
    private void startVerifyPassword(final String pin, final Intent intent) {
        long challenge = getActivity().getIntent().getLongExtra(
                "EXTRA_KEY_CHALLENGE", 0);
        final int localEffectiveUserId = mEffectiveUserId;
        mPendingLockCheck = LockPatternChecker.verifyPassword(
                mLockPatternUtils,
                pin,
                challenge,
                localEffectiveUserId,
                new LockPatternChecker.OnVerifyCallback() {
                    @Override
                    public void onVerified(byte[] token, int timeoutMs) {
                        mPendingLockCheck = null;
                        boolean matched = false;
                        if (token != null) {
                            matched = true;
                            intent.putExtra(
                                    "EXTRA_KEY_CHALLENGE_TOKEN",
                                    token);
                        }
                        onPasswordChecked(matched, intent, timeoutMs, localEffectiveUserId);
                    }
                });
    }

    private void startCheckPassword(final String pin, final Intent intent) {
        final int localEffectiveUserId = mEffectiveUserId;
        mPendingLockCheck = LockPatternChecker.checkPassword(
                mLockPatternUtils,
                pin,
                localEffectiveUserId,
                new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        mPendingLockCheck = null;
                        if (matched && isInternalActivity()) {
                            intent.putExtra("EXTRA_KEY_TYPE",
                                            mIsAlpha ? StorageManager.CRYPT_TYPE_PASSWORD
                                                     : StorageManager.CRYPT_TYPE_PIN);
                            intent.putExtra(
                                    "EXTRA_KEY_PASSWORD", pin);
                        }
                        onPasswordChecked(matched, intent, timeoutMs, localEffectiveUserId);
                    }
                });
    }

    private void showError(int msg) {
        showError(msg, ERROR_MESSAGE_TIMEOUT);
    }

    private final Runnable mResetErrorRunnable = new Runnable() {
        public void run() {
            mErrorTextView.setText("");
        }
    };
    
    private void showError(CharSequence msg, long timeout) {
        mErrorTextView.setText(msg);
        mPasswordEntry.setText(null);
        mHandler.removeCallbacks(mResetErrorRunnable);
        if (timeout != 0) {
            mHandler.postDelayed(mResetErrorRunnable, timeout);
        }
    }

    private void showError(int msg, long timeout) {
        showError(getText(msg), timeout);
    }
    
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (actionId == EditorInfo.IME_NULL
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT) {
            handleNext();
            return true;
        }
        return false;
	}
}
