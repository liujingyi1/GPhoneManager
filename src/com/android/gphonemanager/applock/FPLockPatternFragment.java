package com.android.gphonemanager.applock;

import java.util.List;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternView.Cell;
//import com.android.settingslib.animation.AppearAnimationCreator;
//import com.android.settingslib.animation.AppearAnimationUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.android.gphonemanager.R;
import com.android.gphonemanager.utils.Utils;

public class FPLockPatternFragment extends Fragment {
	
	private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }
	
	private static final String TAG = "GPhoneManager.FPLockPatternFragment";
	
	// how long we wait to clear a wrong pattern
    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

    private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";
	
	private LockPatternView mLockPatternView;
	private TextView mHeaderTextView;
    private TextView mDetailsTextView;
    private TextView mErrorTextView;
	
	private LockPatternUtils mLockPatternUtils;
	private AsyncTask<?, ?, ?> mPendingLockCheck;
	private int mEffectiveUserId;
	private int mNumWrongConfirmAttempts;
	private CountDownTimer mCountdownTimer;
	
	// caller-supplied text for various prompts
    private CharSequence mHeaderText;
    private CharSequence mDetailsText;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLockPatternUtils = new LockPatternUtils(getActivity());
        mEffectiveUserId = Utils.getEffectiveUserId(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fp_lock_pattern_fragment, null);
		mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
        mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
        mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
        mErrorTextView = (TextView) view.findViewById(R.id.errorText);
        
        mLockPatternView.setTactileFeedbackEnabled(
                mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(
                mEffectiveUserId));
        mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
        
        mHeaderText = getString(R.string.lockpassword_confirm_your_pattern_header);
        mDetailsText = getString(R.string.lockpassword_confirm_your_pattern_generic);
        
        // make it so unhandled touch events within the unlock screen go to the
        // lock pattern view.
        final LinearLayoutWithDefaultTouchRecepient topLayout
                = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
        topLayout.setDefaultTouchRecepient(mLockPatternView);
        
        updateStage(Stage.NeedToUnlock);
        
        if (savedInstanceState != null) {
            mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
        } else {
            // on first launch, if no lock pattern is set, then finish with
            // success (don't want user to get stuck confirming something that
            // doesn't exist).
            if (!mLockPatternUtils.isLockPatternEnabled(mEffectiveUserId)) {
                /// M: Add Intent to avoid JE in ChooseLockGeneric
                getActivity().finish();
            }
        }
        
		return view;
	}
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // deliberately not calling super since we are managing this in full
        outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	// if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else if (!mLockPatternView.isEnabled()) {
            // The deadline has passed, but the timer was cancelled. Or the pending lock
            // check was cancelled. Need to clean up.
            mNumWrongConfirmAttempts = 0;
            updateStage(Stage.NeedToUnlock);
        }
    }
    
    
    @Override
    public void onPause() {
        super.onPause();

        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
        }
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
            mPendingLockCheck = null;
        }
    }
    
    /**
     * The pattern listener that responds according to a user confirming
     * an existing lock pattern.
     */
    private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
            = new LockPatternView.OnPatternListener()  {

        public void onPatternStart() {
        	Log.d(TAG, "onPatternStart");
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
        }

        public void onPatternCleared() {
        	Log.d(TAG, "onPatternCleared");
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
        }

        public void onPatternCellAdded(List<Cell> pattern) {
        }

        public void onPatternDetected(List<LockPatternView.Cell> pattern) {
        	Log.d(TAG, "onPatternDetected");
        	mLockPatternView.setEnabled(false);
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
            }

            Intent intent = new Intent();
            startCheckPattern(pattern, intent);
            return;
        }

        private void startVerifyPattern(final List<LockPatternView.Cell> pattern,
                final Intent intent) {
            final int localEffectiveUserId = mEffectiveUserId;
            long challenge = 0;//?
            mPendingLockCheck = LockPatternChecker.verifyPattern(
                    mLockPatternUtils,
                    pattern,
                    challenge,
                    localEffectiveUserId,
                    new LockPatternChecker.OnVerifyCallback() {
                        @Override
                        public void onVerified(byte[] token, int timeoutMs) {
                            mPendingLockCheck = null;
                            boolean matched = false;
                            if (token != null) {
                                matched = true;
                            }
                            onPatternChecked(pattern,
                                    matched, intent, timeoutMs, localEffectiveUserId);
                        }
                    });
        }

        private void startCheckPattern(final List<LockPatternView.Cell> pattern,
                final Intent intent) {
        	Log.d(TAG, "startCheckPattern");
            if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                onPatternChecked(pattern, false, intent, 0, mEffectiveUserId);
                return;
            }

            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.checkPattern(
                    mLockPatternUtils,
                    pattern,
                    localEffectiveUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                        	Log.d(TAG, "LockPatternChecker-onChecked");
                        	mPendingLockCheck = null;
                            onPatternChecked(pattern, matched, intent, timeoutMs,
                                    localEffectiveUserId);
                        }
                    });
        }

        private void onPatternChecked(List<LockPatternView.Cell> pattern,
                boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
        	Log.d(TAG, "onPatternChecked");
        	mLockPatternView.setEnabled(true);
            if (matched) {
            	Log.d(TAG, "onPatternChecked-matched");
            	mErrorTextView.setText("");
            	if (getActivity() instanceof FPRecognizeActivity) {
            		((FPRecognizeActivity)getActivity()).unlock();
            	}
            } else {
            	Log.d(TAG, "onPatternChecked-timeoutMs:"+timeoutMs);
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            effectiveUserId, timeoutMs);
                    handleAttemptLockout(deadline);
                } else {
                    updateStage(Stage.NeedToUnlockWrong);
                    postClearPatternRunnable();
                }
            }
        }
    };
    
    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
    	Log.d(TAG, "handleAttemptLockout");
    	updateStage(Stage.LockedOut);
        long elapsedRealtime = SystemClock.elapsedRealtime();
    	mCountdownTimer = new CountDownTimer(
                elapsedRealtimeDeadline - elapsedRealtime,
                LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsCountdown = (int) (millisUntilFinished / 1000);
                mErrorTextView.setText(getString(
                        R.string.lockpattern_too_many_failed_confirmation_attempts,
                        secondsCountdown));
            }

            @Override
            public void onFinish() {
                mNumWrongConfirmAttempts = 0;
                updateStage(Stage.NeedToUnlock);
            }
        }.start();
    }

	private void updateStage(Stage stage) {
		switch (stage) {
	        case NeedToUnlock:
	            if (mHeaderText != null) {
	                mHeaderTextView.setText(mHeaderText);
	            } else {
	                mHeaderTextView.setText(R.string.lockpassword_confirm_your_pattern_header);
	            }
	            if (mDetailsText != null) {
	                mDetailsTextView.setText(mDetailsText);
	            } else {
	                mDetailsTextView.setText(
	                        R.string.lockpassword_confirm_your_pattern_generic);
	            }
	            mErrorTextView.setText("");
	
	            mLockPatternView.setEnabled(true);
	            mLockPatternView.enableInput();
	            mLockPatternView.clearPattern();
	            break;
	        case NeedToUnlockWrong:
	            mErrorTextView.setText(R.string.lockpattern_need_to_unlock_wrong);
	
	            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
	            mLockPatternView.setEnabled(true);
	            mLockPatternView.enableInput();
	            break;
	        case LockedOut:
	            mLockPatternView.clearPattern();
	            // enabled = false means: disable input, and have the
	            // appearance of being disabled.
	            mLockPatternView.setEnabled(false); // appearance of being disabled
	            break;
	    }

	    // Always announce the header for accessibility. This is a no-op
	    // when accessibility is disabled.
		mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
		
	}
	
	private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };
    
    // clear the wrong pattern unless they have started a new one
    // already
    private void postClearPatternRunnable() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
    }
}
