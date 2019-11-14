package com.android.gphonemanager.applock;

import java.util.HashMap;
import java.util.List;

import com.android.gphonemanager.PhoneManagerProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AppLockModelImpl implements IAppLockModel {
	private static final String TAG = "Gmanager.AppLockModelImpl";

	private static final String URI_LOCK_APP = "content://"
			+ PhoneManagerProvider.AUTHORITIES + "/"
			+ PhoneManagerProvider.TABLE_APP_LOCK;

	Context mContext;

	public AppLockModelImpl(Context mContext) {
		this.mContext = mContext;
	}

	@Override
	public void getLockApps(List<LockApp> apps) {
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI_LOCK_APP);
		Cursor cursor = contentResolver.query(uri, null, null, null, null);
		if (cursor != null) {
			int count = cursor.getCount();
			Log.d(TAG, "count=" + count);
			LockApp app;
			apps.clear();
			while (cursor.moveToNext()) {
				int cursorIndex = cursor
						.getColumnIndex(PhoneManagerProvider.APP_LOCK_IS_LOCKED);
				int isLock = cursor.getInt(cursorIndex);
				cursorIndex = cursor
						.getColumnIndex(PhoneManagerProvider.APP_LOCK_PACKAGE);
				String packageName = cursor.getString(cursorIndex);
				app = new LockApp(packageName, 0);
				apps.add(app);
			}
			cursor.close();
		}
	}

	@Override
	public void lock(LockApp app, List<LockApp> lockedApps) {
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI_LOCK_APP);

		if (app.locked) {
			app.locked = false;
			
			for (LockApp lockApp : lockedApps) {
				if (lockApp.packageName.equals(app.packageName)) {
					lockedApps.remove(lockApp);
					break;
				}
			}

			contentResolver.delete(uri, PhoneManagerProvider.APP_LOCK_PACKAGE
					+ "=?",
					new String[] { app.packageName});
		} else {
			app.locked = true;
			LockApp lockApp = new LockApp(app.packageName, 0);
			lockedApps.add(lockApp);
			ContentValues values = new ContentValues();
			values.put(PhoneManagerProvider.APP_LOCK_PACKAGE,
					app.packageName);
			contentResolver.insert(uri, values);
			
		}
	}

	@Override
	public void lock(LockApp app, HashMap<String, LockApp> lockedAppsMap) {
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI_LOCK_APP);

		if (app.locked) {
			app.locked = false;
			if (lockedAppsMap.containsKey(app.packageName)) {
				lockedAppsMap.remove(app.packageName);
			}

			contentResolver.delete(uri, PhoneManagerProvider.APP_LOCK_PACKAGE
					+ "=?",
					new String[] { app.packageName});
		} else {
			app.locked = true;
			LockApp lockApp = new LockApp(app.packageName, 0);
			lockedAppsMap.put(app.packageName, lockApp);
			ContentValues values = new ContentValues();
			values.put(PhoneManagerProvider.APP_LOCK_PACKAGE,
					app.packageName);
			contentResolver.insert(uri, values);
		}
		
	}

	@Override
	public void getLockApps(HashMap<String, LockApp> lockedAppsMap) {
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI_LOCK_APP);
		Cursor cursor = contentResolver.query(uri, null, null, null, null);
		if (cursor != null) {
			int count = cursor.getCount();
			Log.d(TAG, "count=" + count);
			LockApp app;
			lockedAppsMap.clear();
			while (cursor.moveToNext()) {
				int cursorIndex = cursor
						.getColumnIndex(PhoneManagerProvider.APP_LOCK_IS_LOCKED);
				int isLock = cursor.getInt(cursorIndex);
				cursorIndex = cursor
						.getColumnIndex(PhoneManagerProvider.APP_LOCK_PACKAGE);
				String packageName = cursor.getString(cursorIndex);
				app = new LockApp(packageName, 0);
				lockedAppsMap.put(packageName, app);
			}
			cursor.close();
		}
		
	}

}
