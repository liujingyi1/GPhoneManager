package com.android.gphonemanager.applock;

import static android.view.WindowManager.DOCKED_INVALID;
import static android.app.ActivityManager.StackId.DOCKED_STACK_ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.gphonemanager.R;
import com.android.gphonemanager.applock.LockApp.State;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.IProcessObserver;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.input.InputManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.internal.view.RotationPolicy;

public class AppLockService extends Service {

	private static final String TAG = "Gmanager.AppLockService";

	public final static String APP_LOCK_CMD = "app_lock_cmd";
	public final static int CMD_START_LISTION = 1001;
	public final static int CMD_STOP_LISTION = 1002;
	public final static int CMD_CHECK = 1003;
	public final static int CMD_SEND_HOME = 1004;

	/*
	 * Show lock view when on the system lock screen
	 */
	private static final boolean showOnSystemLockScreen = false;
	
	/*
	 * Show lock view when unluck from system lock screen
	 */
	private static final boolean showAfterSystemLockScreen = true;

	public final static int MSG_CHECK = 101;
	public final static int MSG_SHOW_LOCK = 102;
	public final static int MSG_DISMISS_LOCK = 103;
	public final static int MSG_CHECK_RECENT = 104;
	public final static int MSG_CHECK_HOME = 105;

	public final static int FLAG_DISMISS_BY_UNLOCK = 1;

	/*
	 * DELAY 300 ms for IProcessObserver Mode NO DELAY for mReceiver
	 */
	private final static int SHOW_DELAY_LVL_0 = 1;
	private final static int SHOW_DELAY_LVL_1 = 60;

	private final static String RECENT_ACTIVITY_NAME = "com.android.systemui.recents.RecentsActivity";

	private IAppLockModel model;
	private IApplockService mService = null;

	private FingerprintManager mFingerprintManager;
	private KeyguardManager mKeyguardManager;
	private StatusBarManager mStatusBarManager;
	private IActivityManager mIam;

	private boolean stopListen = false;

	// RotationPolicy, if RotationLock is NOT locked, change it when app lock.
	private boolean isRotationLockChanged = false;

	public static List<LockApp> lockedApps = new ArrayList<>();
	public static HashMap<String, LockApp> lockedAppsMap = new HashMap<String, LockApp>();;
	private LockApp mLockApp;

	// This topActivty maybe is the old one
	private ComponentName preTopActivity;
	// And this is the current one
	private ComponentName currentTopActivity;

	private AppLockViewHelper mViewHelper;
	
	private ComponentName lockedApp;

	/*
	 * Only show lock view again after reboot
	 */
	private boolean lockAgainOnlyAfterReboot = false;
	
	private boolean lockAgainOnlyAfterScreenOff = true;
	
	private Handler H = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_CHECK:
				Log.i(TAG, "MSG_CHECK **");
				check();
				break;
			case MSG_SHOW_LOCK:
				Log.i(TAG, "MSG_SHOW_LOCK **");
				if(mKeyguardManager.isDeviceSecure()) {
					if (!RotationPolicy.isRotationLocked(AppLockService.this)) {
						isRotationLockChanged = true;
						RotationPolicy.setRotationLock(AppLockService.this, true);
					}
	
					setStatusBarExpandEnable(false);
	
					mViewHelper.showLockScreen((ComponentName) msg.obj);
				}
				break;
			case MSG_DISMISS_LOCK:
				Log.i(TAG, "MSG_DISMISS_LOCK");
				if (isRotationLockChanged) {
					isRotationLockChanged = false;
					RotationPolicy.setRotationLock(AppLockService.this, false);
				}

				setStatusBarExpandEnable(true);

				boolean unlock = (msg.arg1 == FLAG_DISMISS_BY_UNLOCK);
				mViewHelper.dismissLockScreen(unlock);
				if (unlock) {
					mLockApp.state = State.UNLOCKED;
				}
				break;
			case MSG_CHECK_RECENT: {
				Log.i(TAG, "MSG_CHECK_RECENT  **");
				ComponentName topActivity = getTopActivity(false);
				Log.i(TAG, "RECENT-topActivity = " + topActivity);
				ComponentName DockedName = getDockedStackTop();
				Log.i(TAG, "RECENT# - DockedName:"+DockedName);
				
				if (topActivity != null || DockedName != null) {
					if(!isLockedApp(topActivity)
							&& !isLockedApp(DockedName)) {
						H.sendEmptyMessage(MSG_DISMISS_LOCK);
					} else {
						Message message = new Message();
						message.what = MSG_SHOW_LOCK;
						message.obj = lockedApp;
						H.sendMessage(message);
					}
				}
				break;
			}
			case MSG_CHECK_HOME: {
				Log.i(TAG, "MSG_CHECK_HOME  *#*");
				ComponentName topActivity = getTopActivity(false);
				Log.i(TAG, "HOME-topActivity = " + topActivity);
				ComponentName DockedName = getDockedStackTop();
				Log.i(TAG, "HOME# - DockedName:"+DockedName);
				if (topActivity != null || DockedName != null) {
					if(!isLockedApp(topActivity)
							&& !isLockedApp(DockedName)) {
						H.sendEmptyMessage(MSG_DISMISS_LOCK);
					} else {
						Message message = new Message();
						message.what = MSG_SHOW_LOCK;
						message.obj = lockedApp;
						H.sendMessage(message);
					}
				}
				break;
			}
			
			default:
				break;
			}
		};
	};

	private ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "onServiceDisconnected-s:"+name);
			Intent intent = new Intent(AppLockService.this,
					AppLockServiceDaemon.class);
			startService(intent);
			bindService(intent, conn, Context.BIND_IMPORTANT);
			Log.d(TAG, "onServiceDisconnected-e:"+name);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected:"+name);
			mService = IApplockService.Stub.asInterface(service);
			try {
				mService.getInfo();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	IApplockService.Stub stub = new IApplockService.Stub() {

		@Override
		public void getInfo() throws RemoteException {
			Log.d(TAG, "stub - getInfo");
		}
	};

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				Log.i(TAG, "SCREEN_OFF");
				lockAllApps();
				H.sendEmptyMessage(MSG_DISMISS_LOCK);
			} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
				Log.i(TAG, "SCREEN_ON");
//				H.sendEmptyMessage(MSG_CHECK);
				if (!mKeyguardManager.isKeyguardLocked()) {
					H.sendEmptyMessage(MSG_CHECK);
				}
			} else if (Intent.ACTION_USER_PRESENT.equals(action)) {
				Log.i(TAG, "ACTION_USER_PRESENT");
				if (showAfterSystemLockScreen) {
					H.sendEmptyMessage(MSG_CHECK);
				} else {
					mViewHelper.restartListeningFingerprintIfNeeded();
				}
			} else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action)) {
				String reason = intent.getStringExtra("reason");
				Log.i(TAG, "reason:" + reason);
				if ("homekey".equals(reason)) {
					Message message = new Message();
					message.what = MSG_CHECK_HOME;
					message.arg1 = 0;
					H.sendMessageDelayed(message, 300);
				} else if ("recentapps".equals(reason)) {
					Message message = new Message();
					message.what = MSG_CHECK_RECENT;
					message.arg1 = 0;
					H.sendMessageDelayed(message, 100);
					// H.sendEmptyMessage(MSG_DISMISS_LOCK);
				}
			}
		}
	};

	private IProcessObserver.Stub mProcessObserver = new IProcessObserver.Stub() {
		@Override
		public void onForegroundActivitiesChanged(int pid, int uid,
				boolean foregroundActivities) {
			Log.d(TAG, "onForegroundActivitiesChanged -- pid:" + pid + ", uid:"
					+ uid + ", foregroundActivities:" + foregroundActivities);
			if (foregroundActivities) {
				H.sendEmptyMessage(MSG_CHECK);
			} else {
				H.sendEmptyMessage(MSG_CHECK);
			}
		}

		@Override
		public void onProcessStateChanged(int pid, int uid, int importance) {
		}

		@Override
		public void onProcessDied(int pid, int uid) {
		}
	};

	public void onCreate() {
		Log.d(TAG, "onCreate");

		Intent intent = new Intent(AppLockService.this,
				AppLockServiceDaemon.class);
		startService(intent);
		bindService(intent, conn, Context.BIND_IMPORTANT);
		
		lockAgainOnlyAfterScreenOff = getResources().getBoolean(R.bool.lock_again_only_after_screenoff);
//		lockAgainOnlyAfterReboot = getResources().getBoolean(R.bool.lock_again_only_after_reboot);
		mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
		mStatusBarManager = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
		mIam = ActivityManagerNative.getDefault();
		model = new AppLockModelImpl(this);
//		model.getLockApps(lockedApps);
		model.getLockApps(lockedAppsMap);
		mViewHelper = new AppLockViewHelper(this, H, mFingerprintManager);

		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_USER_PRESENT);
		filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		registerReceiver(mReceiver, filter);

		try {
			ActivityManagerNative.getDefault().registerProcessObserver(
					mProcessObserver);
		} catch (RemoteException e) {
			Log.e(TAG, " registerProcessObserver occurs RemoteException, ", e);
		}

		mViewHelper.initLockScreen();
	};

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();

		mViewHelper.onDestroy();

		try {
			ActivityManagerNative.getDefault().unregisterProcessObserver(
					mProcessObserver);
		} catch (RemoteException e) {
			Log.e(TAG, " registerProcessObserver occurs RemoteException, ", e);
		}

		unregisterReceiver(mReceiver);
		
		Intent intent = new Intent("com.rgk.fp.APP_LOCK_BOOT_SERVICE");
		intent.setPackage(getPackageName());
		
		startService(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return stub;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand >>>");
		if (intent != null) {
			int cmd = intent.getIntExtra(APP_LOCK_CMD, -1);
			switch (cmd) {
			case CMD_START_LISTION:
				Log.d(TAG, "START_LISTION");
				stopListen = false;
//				if (lockedApps.isEmpty()) {
				if(lockedAppsMap.isEmpty()){
				} else {
					H.sendEmptyMessage(MSG_CHECK);
				}
				break;
			case CMD_STOP_LISTION:
				Log.d(TAG, "CMD_STOP_LISTION");
				stopListen = true;
				break;
			case CMD_CHECK:
				Log.d(TAG, "CMD_CHECK");
				/*
				 * currentTopActivity = new
				 * ComponentName(intent.getStringExtra("package"),
				 * intent.getStringExtra("class")); Log.d(TAG,
				 * "currentTopActivity=" + currentTopActivity); check();
				 */
				break;
			case CMD_SEND_HOME: {
				Log.i(TAG, "CMD_SEND_HOME");
				sendKeyEvent(KeyEvent.KEYCODE_HOME);
				break;
			}
			default:
				break;
			}
		}
		return START_STICKY;
	}

	private ComponentName getTopActivity(boolean fromActivityManager) {
		if (currentTopActivity != null && !fromActivityManager) {
			return currentTopActivity;
		} else {
			// Get from ActivityManager
			ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningTaskInfo> runningTaskInfos = am.getRunningTasks(1);
			RunningTaskInfo runningTaskInfo = runningTaskInfos.get(0);
			return runningTaskInfo.topActivity;
		}
	}
	
	/**
	 * For multi-window
	 */
	public ComponentName getDockedStackTop() {
		if (mIam == null) return null;
		ActivityManager.StackInfo stackInfo = null;
        try {
            stackInfo = mIam.getStackInfo(DOCKED_STACK_ID);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (stackInfo != null) {
            return stackInfo.topActivity;
        }
        return null;
	}
	
	public int getDockSide() {
        try {
            return WindowManagerGlobal.getWindowManagerService().getDockedStackSide();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get dock side: " + e);
        }
        return DOCKED_INVALID;
    }

	private boolean isHomeActivity(ComponentName componentName) {
		PackageManager mPackageManager = getPackageManager();
		Intent queryIntent = new Intent(Intent.ACTION_MAIN);
		queryIntent.addCategory(Intent.CATEGORY_HOME);
		queryIntent.setClassName(componentName.getPackageName(),
				componentName.getClassName());
		List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(
				queryIntent, PackageManager.MATCH_ALL);
		if (resolveInfos.isEmpty()) {
			Log.i(TAG, "isHomeActivity - false");
			return false;
		} else {
			Log.i(TAG, "isHomeActivity - true");
			return true;
		}
	}

	private boolean isFingerprintSetting(ComponentName componentName) {
		if (componentName.getPackageName().equals("com.android.settings")
				&& componentName.getClassName().startsWith(
						"com.android.settings.fingerprint.")) {
			Log.i(TAG, "isFingerprintSetting");
			return true;
		}

		return false;
	}

	private boolean isSettingConfirmLock(ComponentName componentName) {
		if (componentName.getPackageName().equals("com.android.settings")
				&& componentName.getClassName().startsWith(
						"com.android.settings.ConfirmLock")) {
			Log.i(TAG, "isSettingConfirmLock");
			return true;
		}

		return false;
	}

	private int getShowDelay(String packageName) {
		if ("com.google.android.googlequicksearchbox".equals(packageName)
				|| "com.android.stk".equals(packageName)) {
			return SHOW_DELAY_LVL_1;
		}
		return SHOW_DELAY_LVL_0;
	}

	private void check() {
		if (mViewHelper.isLockScreenShow()) {
			Log.d(TAG, "isLockScreenShow");
			return;
		}
		ComponentName topActivity = getTopActivity(false);
		Log.d(TAG, "topActivity=" + topActivity);
		Log.d(TAG, "pre-topActivity=" + preTopActivity);
		Log.d(TAG, "lockedApp number:" + lockedAppsMap.size());
		if (preTopActivity == null) {
			preTopActivity = topActivity;
		}
		
		if(!lockAgainOnlyAfterScreenOff 
				&& lockedAppsMap.containsKey(preTopActivity.getPackageName())
				&& !preTopActivity.equals(topActivity) && !lockAgainOnlyAfterReboot) {
			//lockAllApps();
			lockedAppsMap.get(preTopActivity.getPackageName()).state = State.LOCKED;
		}
		
		ComponentName DockedName = getDockedStackTop();
		Log.i(TAG, "# - DockedName:"+DockedName);
		
		if (isLockedApp(topActivity) || isLockedApp(DockedName)) {
			Message message = new Message();
			message.what = MSG_SHOW_LOCK;
			message.obj = lockedApp;

			Intent intent = new Intent();
			Log.d(TAG, "show on system lock screen:" + showOnSystemLockScreen);
			if (!showOnSystemLockScreen && mKeyguardManager.isKeyguardLocked()) {

			} else {
				H.sendMessage(message);
			}
		}
		preTopActivity = topActivity;
	}

	private boolean isLockedApp(ComponentName componentName) {
		Log.d(TAG, "isLockedApp:" + componentName);
		if (componentName == null) return false;
		if (lockedAppsMap.containsKey(componentName.getPackageName())) {
			LockApp app = lockedAppsMap.get(componentName.getPackageName());
			if (app.state == State.UNLOCKED
					|| isFingerprintSetting(componentName)
					|| isSettingConfirmLock(componentName)) {
				return false;
			} else if (app.state == State.LOCKED) {
				Log.i(TAG, "lock");
				mLockApp = app;
				lockedApp = componentName;
				preTopActivity = componentName;
				return true;
			}
		}
		return false;
	}

	private void setStatusBarExpandEnable(boolean enable) {
		if (enable) {
			mStatusBarManager.disable(StatusBarManager.DISABLE_NONE);
		} else {
			mStatusBarManager.disable(StatusBarManager.DISABLE_EXPAND);
		}
	}
	
	private void lockAllApps() {
		/*for (LockApp lockApp : lockedApps) {
			lockApp.state = State.LOCKED;
		}*/
		if (lockAgainOnlyAfterReboot) {
			return;
		} else {
			for (Map.Entry<String, LockApp> item:lockedAppsMap.entrySet()) {
				item.getValue().state = State.LOCKED;
			}
		}
	}
	
	
	private void sendKeyEvent(int keyCode) {
		Log.d(TAG, "keyCode = " + keyCode);
		long start = SystemClock.uptimeMillis();
		Log.d(TAG, "start = " + start);
		injectKeyEvent(new KeyEvent(start, start, KeyEvent.ACTION_DOWN, keyCode, 0,
				0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
				InputDevice.SOURCE_KEYBOARD));
		injectKeyEvent(new KeyEvent(start, start, KeyEvent.ACTION_UP, keyCode, 0,
				0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
				InputDevice.SOURCE_KEYBOARD));
		long end = SystemClock.uptimeMillis();
		Log.d(TAG, "end - start = " + (end- start));
	}

	private void injectKeyEvent(KeyEvent event) {
		InputManager.getInstance().injectInputEvent(event,
				InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
	}
}
