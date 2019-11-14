package com.android.gphonemanager.freezeapp;

import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.PhoneManagerProvider;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public class BootCmpleteReceiver extends BroadcastReceiver {

	private static final String TAG = "BootCmpleteReceiver";
	private static final String URI = "content://"
			+ PhoneManagerProvider.AUTHORITIES + "/"
			+ PhoneManagerProvider.TABLE_FREEZE_APPS;
	private static final String IS_FIRST_BOOT = "is_first_boot";
	
	Context mContext;
	List<String> mPackageList;
	PackageManager mPackageManager;
	protected Thread deleteThread;
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.i(TAG, "[onReceive], action is " + action);
        
        mContext = context;
        
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        boolean isRunning = userManager.isUserRunning(new UserHandle(UserHandle.myUserId()));
        Log.d(TAG, "[onReceive], the current user is: " + UserHandle.myUserId()
                + " isRunning: " + isRunning);
        if (!isRunning) {
            return;
        }
        
        SharedPreferences perferences = context.getSharedPreferences(context.getPackageName(),
                Context.MODE_PRIVATE);
        
        boolean isFirst = perferences.getBoolean(
        		IS_FIRST_BOOT, true);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
        	
        	if (!isFirst) {
        		checkFreezeApp();
        	}
        }
        
        perferences.edit().putBoolean(IS_FIRST_BOOT, false).apply();

	}
	
	public void setApplicationDisable(String packageName) {
		if (mPackageManager == null) {
			mPackageManager = mContext.getPackageManager();
		}
		
		mPackageManager.setApplicationEnabledSetting(packageName,
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
	}
	
	private int getFreezeAppState(String packageName) {
		if (mPackageManager == null) {
			mPackageManager = mContext.getPackageManager();
		}
		
		return mPackageManager.getApplicationEnabledSetting(packageName);
	}
	
	private void checkFreezeApp() {
		deleteThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				ContentResolver contentResolver = mContext.getContentResolver();
				Uri uri = Uri.parse(URI);
				Cursor cursor = contentResolver.query(uri, null, null, null, null);
				
				if (cursor != null) {
					
					mPackageList = new ArrayList<String>(); 
					
					while (cursor.moveToNext()) {
						int cursorIndex = cursor
								.getColumnIndex(PhoneManagerProvider.FREEZE_PACKAGE_NAME);
						String packageName = cursor.getString(cursorIndex);
						
						mPackageList.add(packageName);
						
					}
					cursor.close();
				}
				
				for (String packageName : mPackageList) {
					int state = getFreezeAppState(packageName);
					Log.i(TAG, "FreezeApp pacakgeName="+packageName+" state="+state);
					if (PackageManager.COMPONENT_ENABLED_STATE_DISABLED != state) {
						Log.i(TAG, "FreezeApp error state: pacakgeName="+packageName+" state="+state);
						setApplicationDisable(packageName);
					}
				}
				
			}
		});
		deleteThread.start();
	}

}
