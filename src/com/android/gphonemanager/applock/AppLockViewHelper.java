package com.android.gphonemanager.applock;

import java.util.List;

import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.internal.widget.LockPatternView.Cell;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.gphonemanager.R;
import com.android.gphonemanager.applock.FpmAuthenticationCallback.OnAuthenticationSucceeded;
import com.android.gphonemanager.applock.LockApp.State;
import com.android.gphonemanager.utils.Utils;
import com.android.gphonemanager.view.LockView;
import com.android.gphonemanager.view.LockView.OnBackKeyPressedListener;

public class AppLockViewHelper {
	
	private enum Stage {
		NeedToUnlock, NeedToUnlockWrong, LockedOut
	}

	private enum ViewState {
		Fingerprint, Password, Pattern, PasswordOnly, PatternOnly
	}
	
	private static final String TAG = "Gmanager.AppLockViewHelper";
	
	private final static int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;
	private final static long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;

	private final static String KEY_NUM_WRONG_CONFIRM_ATTEMPTS = "confirm_lock_password_fragment.key_num_wrong_confirm_attempts";

	private final static long ERROR_MESSAGE_TIMEOUT = 3000;

	// how long we wait to clear a wrong pattern
	private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;
	private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";
	
	private static boolean isLockScreenShow = false;
	private static Object lock = new Object();
	
	private Context mContext;
	private WindowManager mWindowManager;
	private FingerprintManager mFingerprintManager;
	private Handler H;
	private WindowManager.LayoutParams mLockViewParams;
	private LockView mLockView;
	private View mLockViewTitle;
	private ViewState mViewState;
	private boolean isAnim = false;
	
	private int orientation = Configuration.ORIENTATION_PORTRAIT;
	private int SCREEN_WIDTH;
	private int SCREEN_HEIGHT;
	private int SCREEN_WIDTH_H;
	private int SCREEN_HEIGHT_H;

	// Fingerprint view
	private View lockFpView;
	private TextView textPrompt;
	private TextView switchView;
	private View iconView;
	private ImageView appImageView;

	// Password view
	private View lockPasswordView;
	private TextView mPasswordEntry;
	private TextViewInputDisabler mPasswordEntryInputDisabler;
	private TextView mHeaderTextView;
	private TextView mDetailsTextView;
	private TextView mErrorTextView;

	// Pattern view
	private LinearLayoutWithDefaultTouchRecepient lockPatternView;
	private LockPatternView mLockPatternView;
	private TextView mPatternHeaderTextView;
	private TextView mPatternDetailsTextView;
	private TextView mPatternErrorTextView;

	// Fingerprint
	private FpmAuthenticationCallback mCallBack;
	private int mIconTouchCount;
	private LockPatternUtils mLockPatternUtils;
	private int mEffectiveUserId;

	// Password
	private InputMethodManager mImm;
	private AsyncTask<?, ?, ?> mPendingLockCheck;
	private int mNumWrongConfirmAttempts;
	private boolean mIsAlpha;
	private CountDownTimer mCountdownTimer;
	private int defInputType;

	// Pattern
	// caller-supplied text for various prompts
	private CharSequence mHeaderText;
	private CharSequence mDetailsText;
	
	
	public AppLockViewHelper (Context context, Handler handler, FingerprintManager fingerprintManager) {
		mContext = context;
		H = handler;
		mFingerprintManager = fingerprintManager;
		mWindowManager  = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		mLockPatternUtils = new LockPatternUtils(mContext);
		mEffectiveUserId = Utils.getEffectiveUserId(mContext);
		
		Point size = new Point();
		mWindowManager.getDefaultDisplay().getSize(size);
		SCREEN_WIDTH = size.x;
		SCREEN_HEIGHT= size.y 
				+ mContext.getResources().getDimensionPixelOffset(R.dimen.delta_height);
		SCREEN_WIDTH_H = size.y;
		SCREEN_HEIGHT_H = size.x
				+ mContext.getResources().getDimensionPixelOffset(R.dimen.delta_height_h);
		Log.d(TAG, "SCREEN_WIDTH="+SCREEN_WIDTH);
		Log.d(TAG, "SCREEN_HEIGHT="+SCREEN_HEIGHT);
		Log.d(TAG, "SCREEN_WIDTH_H="+SCREEN_WIDTH_H);
		Log.d(TAG, "SCREEN_HEIGHT_H="+SCREEN_HEIGHT_H);
	}
	
	public void onDestroy() {
		if (mLockView != null) {
			if (isLockScreenShow) {
				if (mWindowManager == null) {
					mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
				}

				mWindowManager.removeView(mLockView);
				isLockScreenShow = false;
			}
			mLockView = null;
		}
	}
	
	public boolean isLockScreenShow() {
		return isLockScreenShow;
	}
	
	View.OnClickListener mOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			int id = v.getId();
			switch (id) {
			case R.id.btn_switch: {
				showNotFpLockView(false);
				mPasswordEntry.setEnabled(true);
				mPasswordEntryInputDisabler.setInputEnabled(true);
				if (shouldAutoShowSoftKeyboard()) {
					mImm.showSoftInput(mPasswordEntry,
							InputMethodManager.SHOW_IMPLICIT);
				}

				break;
			}

			default:
				break;
			}
		}
	};
	
	private final Runnable mShowDialogRunnable = new Runnable() {
		@Override
		public void run() {
			showIconTouchDialog();
		}
	};

	private void showIconTouchDialog() {
		mIconTouchCount = 0;
	}
	
	public void initFpView(View lockView) {
		lockFpView = lockView.findViewById(R.id.applock_lock_fp_view);
		textPrompt = (TextView) lockFpView.findViewById(R.id.ptext);
		switchView = (TextView) lockFpView.findViewById(R.id.btn_switch);
		switchView.setOnClickListener(mOnClickListener);
		mCallBack = new FpmAuthenticationCallback(mFingerprintManager,
				textPrompt, new OnAuthenticationSucceeded() {
					@Override
					public void onSucceeded() {
						Log.v(TAG, "fp authentication succeeded");
						Message message = new Message();
						message.what = AppLockService.MSG_DISMISS_LOCK;
						message.arg1 = AppLockService.FLAG_DISMISS_BY_UNLOCK;
						if (!isAnim) {
							H.sendMessage(message);
						} else {
							Log.v(TAG, "isAnim="+isAnim);
							H.sendMessageDelayed(message, 500);
						}
					}
				});

		iconView = lockFpView.findViewById(R.id.icon);
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
						iconView.postDelayed(mShowDialogRunnable,
								ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN);
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

		appImageView = (ImageView) lockFpView.findViewById(R.id.app_image);
	}
	
	public void initPasswordView(View lockView) {
		lockPasswordView = lockView
				.findViewById(R.id.applock_lock_password_view);

		final int storedQuality = mLockPatternUtils
				.getKeyguardStoredPasswordQuality(mEffectiveUserId);

		mPasswordEntry = (TextView) lockPasswordView
				.findViewById(R.id.password_entry);
		mPasswordEntry.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| actionId == EditorInfo.IME_ACTION_NEXT) {
					handleNext();
					return true;
				}
				return false;
			}
		});
		mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

		mHeaderTextView = (TextView) lockPasswordView
				.findViewById(R.id.headerText);
		mDetailsTextView = (TextView) lockPasswordView
				.findViewById(R.id.detailsText);
		mErrorTextView = (TextView) lockPasswordView
				.findViewById(R.id.errorText);

		mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
				|| DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
				|| DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;

		mHeaderTextView.setText(getDefaultHeader());
		mDetailsTextView.setText(getDefaultDetails());

		defInputType = mPasswordEntry.getInputType();
		mPasswordEntry
				.setInputType(mIsAlpha ? defInputType
						: (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
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

	private void handleNext() {
		mPasswordEntryInputDisabler.setInputEnabled(false);
		if (mPendingLockCheck != null) {
			mPendingLockCheck.cancel(false);
		}

		final String pin = mPasswordEntry.getText().toString();
		if (pin.isEmpty()) {
			Log.v(TAG, "pin is EMPTY");
			mPasswordEntryInputDisabler.setInputEnabled(true);
		} else {
			Intent intent = new Intent();
			startCheckPassword(pin, intent);
		}
	}
	
	private void startCheckPassword(final String pin, final Intent intent) {
		final int localEffectiveUserId = mEffectiveUserId;
		mPendingLockCheck = LockPatternChecker.checkPassword(mLockPatternUtils,
				pin, localEffectiveUserId,
				new LockPatternChecker.OnCheckCallback() {
					@Override
					public void onChecked(boolean matched, int timeoutMs) {
						mPendingLockCheck = null;
						onPasswordChecked(matched, intent, timeoutMs,
								localEffectiveUserId);
					}
				});
	}

	private void onPasswordChecked(boolean matched, Intent intent,
			int timeoutMs, int effectiveUserId) {
		mPasswordEntryInputDisabler.setInputEnabled(true);
		if (matched) {
			if (mImm != null) {
				mImm.hideSoftInputFromWindow(
						mLockView.getApplicationWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
			Message message = new Message();
			message.what = AppLockService.MSG_DISMISS_LOCK;
			message.arg1 = AppLockService.FLAG_DISMISS_BY_UNLOCK;
			H.sendMessage(message);
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
	
	private void handleAttemptLockout(long elapsedRealtimeDeadline) {
		long elapsedRealtime = SystemClock.elapsedRealtime();
		Log.d(TAG, "handleAttemptLockout, " + elapsedRealtime);
		mPasswordEntry.setEnabled(false);
		mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline
				- elapsedRealtime,
				LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {
			@Override
			public void onTick(long millisUntilFinished) {
				Log.d(TAG, "Pattern - onTick, " + millisUntilFinished);
				final int secondsCountdown = (int) (millisUntilFinished / 1000);
				showError(
						mContext.getString(
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

	private void resetState() {
		mPasswordEntry.setEnabled(true);
		mPasswordEntryInputDisabler.setInputEnabled(true);
		if (shouldAutoShowSoftKeyboard()) {
			mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
		}
	}

	private boolean shouldAutoShowSoftKeyboard() {
		return mPasswordEntry.isEnabled();
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
		H.removeCallbacks(mResetErrorRunnable);
		if (timeout != 0) {
			H.postDelayed(mResetErrorRunnable, timeout);
		}
	}

	private void showError(int msg, long timeout) {
		showError(mContext.getText(msg), timeout);
	}
	
	public void initPatternView(View lockView) {
		lockPatternView = (LinearLayoutWithDefaultTouchRecepient) lockView
				.findViewById(R.id.applock_lock_pattern_view);
		mPatternHeaderTextView = (TextView) lockPatternView
				.findViewById(R.id.patternHeaderText);
		mLockPatternView = (LockPatternView) lockPatternView
				.findViewById(R.id.lockPattern);
		mPatternDetailsTextView = (TextView) lockPatternView
				.findViewById(R.id.patternDetailsText);
		mPatternErrorTextView = (TextView) lockPatternView
				.findViewById(R.id.patternErrorText);

		mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils
				.isTactileFeedbackEnabled());
		mLockPatternView.setInStealthMode(!mLockPatternUtils
				.isVisiblePatternEnabled(mEffectiveUserId));
		mLockPatternView
				.setOnPatternListener(mConfirmExistingLockPatternListener);

		mHeaderText = mContext.getString(R.string.lockpassword_confirm_your_pattern_header);
		mDetailsText = mContext.getString(R.string.lockpassword_confirm_your_pattern_generic);

		// make it so unhandled touch events within the unlock screen go to the
		// lock pattern view.
		lockPatternView.setDefaultTouchRecepient(mLockPatternView);

		updateStage(Stage.NeedToUnlock);
	}

	/**
	 * The pattern listener that responds according to a user confirming an
	 * existing lock pattern.
	 */
	private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener = new LockPatternView.OnPatternListener() {
		public void onPatternStart() {
			Log.d(TAG, "onPatternStart");
			mLockPatternView.removeCallbacks(mClearPatternRunnable);
		}

		public void onPatternCleared() {
			Log.d(TAG, "onPatternCleared");
			mLockPatternView.removeCallbacks(mClearPatternRunnable);
		}

		public void onPatternCellAdded(List<Cell> pattern) {
			Log.d(TAG, "onPatternCellAdded");
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

		private void startVerifyPattern(
				final List<LockPatternView.Cell> pattern, final Intent intent) {
			Log.d(TAG, "startVerifyPattern");
			final int localEffectiveUserId = mEffectiveUserId;
			long challenge = 0;// ?
			mPendingLockCheck = LockPatternChecker.verifyPattern(
					mLockPatternUtils, pattern, challenge,
					localEffectiveUserId,
					new LockPatternChecker.OnVerifyCallback() {
						@Override
						public void onVerified(byte[] token, int timeoutMs) {
							Log.d(TAG, "startVerifyPattern onVerified");
							mPendingLockCheck = null;
							boolean matched = false;
							if (token != null) {
								matched = true;
							}
							onPatternChecked(pattern, matched, intent,
									timeoutMs, localEffectiveUserId);
						}
					});
		}
		
		private void startCheckPattern(
				final List<LockPatternView.Cell> pattern, final Intent intent) {
			Log.d(TAG, "startCheckPattern");
			if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
				onPatternChecked(pattern, false, intent, 0, mEffectiveUserId);
				return;
			}
			final int localEffectiveUserId = mEffectiveUserId;
			mPendingLockCheck = LockPatternChecker.checkPattern(
					mLockPatternUtils, pattern, localEffectiveUserId,
					new LockPatternChecker.OnCheckCallback() {
						@Override
						public void onChecked(boolean matched, int timeoutMs) {
							Log.d(TAG, "LockPatternChecker-onChecked");
							mPendingLockCheck = null;
							onPatternChecked(pattern, matched, intent,
									timeoutMs, localEffectiveUserId);
						}
					});
		}

		private void onPatternChecked(List<LockPatternView.Cell> pattern,
				boolean matched, Intent intent, int timeoutMs,
				int effectiveUserId) {
			Log.d(TAG, "onPatternChecked");
			mLockPatternView.setEnabled(true);
			if (matched) {
				Log.d(TAG, "onPatternChecked-matched");
				mPatternErrorTextView.setText("");
				Message message = new Message();
				message.what = AppLockService.MSG_DISMISS_LOCK;
				message.arg1 = AppLockService.FLAG_DISMISS_BY_UNLOCK;
				H.sendMessage(message);
			} else {
				Log.d(TAG, "onPatternChecked-timeoutMs:" + timeoutMs);
				if (timeoutMs > 0) {
					long deadline = mLockPatternUtils
							.setLockoutAttemptDeadline(effectiveUserId,
									timeoutMs);
					handleAttemptLockoutPattern(deadline);
				} else {
					updateStage(Stage.NeedToUnlockWrong);
					postClearPatternRunnable();
				}
			}
		}
	};

	private void handleAttemptLockoutPattern(long elapsedRealtimeDeadline) {
		Log.d(TAG, "handleAttemptLockoutPattern");
		updateStage(Stage.LockedOut);
		long elapsedRealtime = SystemClock.elapsedRealtime();
		mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline
				- elapsedRealtime,
				LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {
			@Override
			public void onTick(long millisUntilFinished) {
				final int secondsCountdown = (int) (millisUntilFinished / 1000);
				mPatternErrorTextView
						.setText(mContext.getString(
								R.string.lockpattern_too_many_failed_confirmation_attempts,
								secondsCountdown));
			}

			@Override
			public void onFinish() {
				updateStage(Stage.NeedToUnlock);
			}
		}.start();
	}

	private void updateStage(Stage stage) {
		Log.d(TAG, "updateStage stage:" + stage);
		switch (stage) {
		case NeedToUnlock:
			if (mHeaderText != null) {
				mPatternHeaderTextView.setText(mHeaderText);
			} else {
				mPatternHeaderTextView
						.setText(R.string.lockpassword_confirm_your_pattern_header);
			}
			if (mDetailsText != null) {
				mPatternDetailsTextView.setText(mDetailsText);
			} else {
				mPatternDetailsTextView
						.setText(R.string.lockpassword_confirm_your_pattern_generic);
			}
			mPatternErrorTextView.setText("");

			mLockPatternView.setEnabled(true);
			mLockPatternView.enableInput();
			mLockPatternView.clearPattern();
			break;
		case NeedToUnlockWrong:
			mPatternErrorTextView
					.setText(R.string.lockpattern_need_to_unlock_wrong);

			mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
			mLockPatternView.setEnabled(true);
			mLockPatternView.enableInput();
			break;
		case LockedOut:
			mLockPatternView.clearPattern();
			mLockPatternView.setEnabled(false); // appearance of being disabled
			break;
		}

		mPatternHeaderTextView.announceForAccessibility(mPatternHeaderTextView
				.getText());

	}
	
	private Runnable mClearPatternRunnable = new Runnable() {
		public void run() {
			Log.d(TAG, "mClearPatternRunnable run");
			mLockPatternView.clearPattern();
		}
	};

	// clear the wrong pattern unless they have started a new one already
	private void postClearPatternRunnable() {
		Log.d(TAG, "postClearPatternRunnable");
		mLockPatternView.removeCallbacks(mClearPatternRunnable);
		mLockPatternView.postDelayed(mClearPatternRunnable,
				WRONG_PATTERN_CLEAR_TIMEOUT_MS);
	}
	
	private void checkDeadline() {
		long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
		Log.d(TAG, "checkDeadline, " + deadline);
        if (deadline != 0) {
        	handleAttemptLockout(deadline);
        } else {
        	resetState();
            mErrorTextView.setText("");
        }
	}
	
	private void checkDeadlinePattern() {
		long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
		Log.d(TAG, "checkDeadlinePattern, " + deadline);
        if (deadline != 0) {
        	handleAttemptLockoutPattern(deadline);
        } else {
        	resetState();
            mPatternErrorTextView.setText("");
        }
	}
	
	private OnBackKeyPressedListener mOnBackKeyPressedListener = new OnBackKeyPressedListener() {

		@Override
		public void pressed() {
			if (!isAnim) {
				if (mViewState == ViewState.Fingerprint
						|| mViewState == ViewState.PasswordOnly
						|| mViewState == ViewState.PatternOnly) {
//					sendKeyEvent(KeyEvent.KEYCODE_HOME);
					Intent intent = new Intent("com.rgk.fp.APP_LOCK_BOOT_SERVICE");
					intent.putExtra(AppLockService.APP_LOCK_CMD, AppLockService.CMD_SEND_HOME);
					intent.setPackage(mContext.getPackageName());
					mContext.startService(intent);
					mLockView.setOnBackKeyPressedListener(null);
				} else if (mViewState == ViewState.Password
						|| mViewState == ViewState.Pattern) {
					lockFpView.setVisibility(View.VISIBLE);
					lockPasswordView.setVisibility(View.GONE);
					lockPatternView.setVisibility(View.GONE);
					mViewState = ViewState.Fingerprint;
				}
			} else {
				Log.v(TAG, "BackKeyPressed, isAnim="+isAnim);
			}
		}
	};
	
	private void updateLockScreenParams() {
		Log.d(TAG, "updateLockScreenParams");
		if (mLockViewParams == null) {
			initLockViewParams();
		} else {
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mLockViewParams.width = SCREEN_WIDTH_H;
				mLockViewParams.height = SCREEN_HEIGHT_H;
				mLockViewParams.screenOrientation = Configuration.ORIENTATION_LANDSCAPE;
			} else {
				mLockViewParams.width = SCREEN_WIDTH;
				mLockViewParams.height = SCREEN_HEIGHT;
				mLockViewParams.screenOrientation = Configuration.ORIENTATION_PORTRAIT;
			}
		}
	}
	
	private void initLockViewParams() {
		Log.d(TAG, "initLockViewParams");
		mLockViewParams = new WindowManager.LayoutParams(
				SCREEN_WIDTH,
				SCREEN_HEIGHT,
				WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
				WindowManager.LayoutParams.FLAG_FULLSCREEN
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.RGBA_8888);
		mLockViewParams.gravity = (Gravity.CENTER);
	}

	public void initLockScreen() {
		Log.d(TAG, "initLockScreen");
		if (mLockViewParams == null) {
			initLockViewParams();
		}
		
		if (mLockView == null) {
			mLockView = (LockView) LayoutInflater.from(mContext)
					.inflate(R.layout.applock_lock_view, null);
			Log.d(TAG, "init view:" + mLockView);
			mLockViewTitle = mLockView.findViewById(R.id.title_bar);
			mLockView
					.setOnBackKeyPressedListener(mOnBackKeyPressedListener);
			initFpView(mLockView);
			initPasswordView(mLockView);
			initPatternView(mLockView);
		}
	}
	
	private void updateLayout(boolean isLandscape) {
		mLockViewTitle.setVisibility(isLandscape? View.GONE : View.VISIBLE);
	}
	
	public void showLockScreen(ComponentName activity) {
		Log.d(TAG, "showLockScreen->mLockView:" + mLockView);
		synchronized (lock) {
			
			int currentO = mContext.getResources().getConfiguration().orientation;
			Log.d(TAG, "orientation="+orientation + "\ncurrentO="+currentO);
			
			if (orientation != currentO) {
				orientation = currentO;
				updateLockScreenParams();
			}
			
			if (mLockView == null) {
				initLockScreen();
			} else {
				mLockView.setOnBackKeyPressedListener(mOnBackKeyPressedListener);
				textPrompt.setText("");
				
				// do something, init again;
				mEffectiveUserId = Utils.getEffectiveUserId(mContext);

				// Password
				final int storedQuality = mLockPatternUtils
						.getKeyguardStoredPasswordQuality(mEffectiveUserId);

				mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
						|| DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
						|| DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;

				mHeaderTextView.setText(getDefaultHeader());
				mDetailsTextView.setText(getDefaultDetails());

				defInputType = mPasswordEntry.getInputType();
				mPasswordEntry
						.setInputType(mIsAlpha ? defInputType
								: (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
				mPasswordEntry.setText(null);

				// Pattern
				mLockPatternView.setInStealthMode(!mLockPatternUtils
						.isVisiblePatternEnabled(mEffectiveUserId));
				mLockPatternView.clearPattern();
				
				lockPasswordView.setScaleX(1);
				lockPasswordView.setScaleY(1);
				lockPasswordView.setAlpha(1);
				
				lockPatternView.setScaleX(1);
				lockPatternView.setScaleY(1);
				lockPatternView.setAlpha(1);
			}

			if (mLockView != null) {
				if (mWindowManager == null) {
					mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
				}

				Log.d(TAG, "showLockScreen->isLockScreenShow:"
						+ isLockScreenShow);
				if (!isLockScreenShow) {
					isLockScreenShow = true;
					updateLayout(orientation == Configuration.ORIENTATION_LANDSCAPE);
					mWindowManager.addView(mLockView, mLockViewParams);
					if (mFingerprintManager.hasEnrolledFingerprints()) {
						lockFpView.setVisibility(View.VISIBLE);
						lockPasswordView.setVisibility(View.GONE);
						lockPatternView.setVisibility(View.GONE);

						mViewState = ViewState.Fingerprint;

						appImageView
								.setImageDrawable(getActivityDrawable(activity));

						mCallBack.startListening(null);
					} else {
						showNotFpLockView(true);
					}
                    
					animShow();
				}
			} else {
				Log.w(TAG, "mLockView:" + mLockView);
			}
		}
	}
	
	public void dismissLockScreen(boolean unlocked) {
		Log.d(TAG, "dismissLockScreen");
		synchronized (lock) {
			if (mLockView != null) {
				if (isLockScreenShow) {
					
					if (mCallBack != null) {
						mCallBack.stopListening();
					}

					if (mCountdownTimer != null) {
						mCountdownTimer.cancel();
						mCountdownTimer = null;
					}
					if (mPendingLockCheck != null) {
						mPendingLockCheck.cancel(false);
						mPendingLockCheck = null;
					}
					
					if (mWindowManager == null) {
						mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
					}
					
					if (unlocked) {
						animDismiss();
					} else {
						if (isLockScreenShow) {
							mWindowManager.removeView(mLockView);
							isLockScreenShow = false;
						}
					}
				}
			}
		}
	}
	
	public void restartListeningFingerprintIfNeeded() {
		if (isLockScreenShow && mViewState != ViewState.PasswordOnly
				&& mViewState != ViewState.PatternOnly
				&& mCallBack != null) {
			mCallBack.stopListening();
			mCallBack.startListening(null);
		}
	}
	
	private void showNotFpLockView(boolean noFingerprint) {
		
		switch (mLockPatternUtils
				.getKeyguardStoredPasswordQuality(mEffectiveUserId)) {
		case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
			lockFpView.setVisibility(View.GONE);
			lockPasswordView.setVisibility(View.GONE);
			lockPatternView.setVisibility(View.VISIBLE);
			if (noFingerprint) {
				mViewState = ViewState.PatternOnly;
			} else {
				mViewState = ViewState.Pattern;
			}
			checkDeadlinePattern();
			break;
		case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
		case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
		case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
		case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
		case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
			lockFpView.setVisibility(View.GONE);
			lockPasswordView.setVisibility(View.VISIBLE);
			lockPatternView.setVisibility(View.GONE);
			if (noFingerprint) {
				mViewState = ViewState.PasswordOnly;
			} else {
				mViewState = ViewState.Password;
			}
			checkDeadline();
			mPasswordEntry.setFocusable(true);
			mPasswordEntry.requestFocus();
			break;
		default:
			return;
		}
	}
	
	private void animShow() {
		View v = getCurrentLockView();
		ObjectAnimator oaSx = ObjectAnimator.ofFloat(v, "scaleX", 3f, 1f);
		ObjectAnimator oaSy = ObjectAnimator.ofFloat(v, "scaleY", 3f, 1f);
		ObjectAnimator oaA = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f);

		AnimatorSet set = new AnimatorSet();
		set.playTogether(oaSx, oaSy, oaA);
		set.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				isAnim = true;
			}
			@Override
			public void onAnimationRepeat(Animator animation) {}
			@Override
			public void onAnimationEnd(Animator animation) {
				isAnim = false;
			}
			@Override
			public void onAnimationCancel(Animator animation) {
				isAnim = false;
			}
		});
		set.setDuration(500).start();
	}
	
	private void animDismiss() {
		View v = getCurrentLockView();
		ObjectAnimator oaSx = ObjectAnimator.ofFloat(v, "scaleX", 1f, 3f);
		ObjectAnimator oaSy = ObjectAnimator.ofFloat(v, "scaleY", 1f, 3f);
		ObjectAnimator oaA = ObjectAnimator.ofFloat(v, "alpha", 1f, 0f);

		AnimatorSet set = new AnimatorSet();
		set.playTogether(oaSx, oaSy, oaA);
		set.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {
				isAnim = true;
			}
			@Override
			public void onAnimationRepeat(Animator animation) {}
			@Override
			public void onAnimationEnd(Animator animation) {
				isAnim = false;
				if (isLockScreenShow) {
					mWindowManager.removeView(mLockView);
					isLockScreenShow = false;
				}
			}
			@Override
			public void onAnimationCancel(Animator animation) {
				isAnim = false;
				if (isLockScreenShow) {
					mWindowManager.removeView(mLockView);
					isLockScreenShow = false;
				}
			}
		});
		set.setDuration(500).start();
	}
	
	private void animSwitch(boolean fromFpView) {
		
	}
	
	private View getCurrentLockView() {
		if (mViewState == ViewState.Fingerprint) {
			return lockFpView;
		} else if (mViewState == ViewState.Password
				|| mViewState == ViewState.PasswordOnly) {
			return lockPasswordView;
		} else {
			return lockPatternView;
		}
	}

	private Drawable getActivityDrawable(ComponentName activity) {
		if (activity == null) {
			return null;
		}

		PackageManager packageManager = mContext.getPackageManager();
		Intent intent = new Intent();
		intent.setClassName(activity.getPackageName(), activity.getClassName());
		List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_ALL);
		if (resolveInfos.isEmpty()) {
			return null;
		} else {
			
			Log.i(TAG,
					"getActivityIconByTheme: \npackageName="
							+ activity.getPackageName() + "\nclassName="
							+ activity.getClassName());
			 return resolveInfos.get(0).loadIcon(packageManager);
//			return resolveInfos.get(0).getDrawableFromTheme(mContext,
//					packageManager, activity.getClassName());
		}
	}

	private void sendKeyEvent(int keyCode) {
		Log.d(TAG, "keyCode = " + keyCode);
		long now = SystemClock.uptimeMillis();
		injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0,
				0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
				InputDevice.SOURCE_KEYBOARD));
		injectKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0,
				0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
				InputDevice.SOURCE_KEYBOARD));
	}

	private void injectKeyEvent(KeyEvent event) {
		InputManager.getInstance().injectInputEvent(event,
				InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
	}
}
