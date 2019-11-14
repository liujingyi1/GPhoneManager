package com.android.gphonemanager.freezeapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.gphonemanager.PhoneManagerProvider;
import com.android.gphonemanager.R;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class FreezeModelImpl implements IFreezeModel {
	private static final String TAG = "Gmanager.FreezeModelImpl";

	private static final String URI = "content://"
			+ PhoneManagerProvider.AUTHORITIES + "/"
			+ PhoneManagerProvider.TABLE_FREEZE_APPS;
	
	private boolean TEST = false;

	private Context mContext;

	private List<FreezeAppInfo> freezeAppInfoList;
	private List<FreezeAppInfo> normalAppInfoList;
	
	private static final String[] skipPackage = {"com.android.settings",
			"com.android.fingerprintfeatures",
			"com.android.gphonemanager",
			"com.android.mhlsettings",
			"com.google.android.dialer",
			"com.google.android.apps.messaging",
			"com.android.documentsui",
			"com.google.android.googlequicksearchbox",
			"com.google.android.apps.photos",
			"com.mediatek.GoogleOta",
			};

	public FreezeModelImpl(Context context) {
		this.mContext = context;
		
		TEST = mContext.getResources().getBoolean(R.bool.isTest);
		
		freezeAppInfoList = new ArrayList<FreezeAppInfo>();
		normalAppInfoList = new ArrayList<FreezeAppInfo>();
	}

	@Override
	public List<FreezeAppInfo> getFreezeApps() {
		FreezePackageManagerAdapter mAdapter = FreezePackageManagerAdapter
				.getInstance(mContext);
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI);
		Cursor cursor = contentResolver.query(uri, null, null, null, null);
		freezeAppInfoList.clear();
		FreezeAppInfo app;
		if (cursor != null) {
			while (cursor.moveToNext()) {
				int cursorIndex = cursor
						.getColumnIndex(PhoneManagerProvider.FREEZE_PACKAGE_NAME);
				String packageName = cursor.getString(cursorIndex);
				ApplicationInfo appInfo = mAdapter.getApplicationInfo(
						packageName, 0);
				
				if (appInfo != null) {
					app = new FreezeAppInfo();
					app.applicationInfo = appInfo;
					app.name = appInfo.loadLabel(mContext.getPackageManager());
					app.icon = appInfo.loadIcon(mContext.getPackageManager());
					app.isFreezed = true;
					freezeAppInfoList.add(app);
				}
			}
			cursor.close();
		}
		return freezeAppInfoList;
	}

	@Override
	public void deleteFreezeApp(int uid, FreezeAppInfo info) {
		FreezePackageManagerAdapter mAdapter = FreezePackageManagerAdapter
				.getInstance(mContext);
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI);
		contentResolver.delete(uri, PhoneManagerProvider.FREEZE_UID + "=?",
				new String[] { String.valueOf(uid) });
		if (!TEST) {
			mAdapter.setApplicationEnable(info.applicationInfo.packageName);
		}
		freezeAppInfoList.remove(info);
		info.isFreezed = false;
		normalAppInfoList.add(info);
	}

	@Override
	public void addFreezeApp(int uid, FreezeAppInfo info) {
		FreezePackageManagerAdapter mAdapter = FreezePackageManagerAdapter
				.getInstance(mContext);
		ContentResolver contentResolver = mContext.getContentResolver();
		Uri uri = Uri.parse(URI);
		ContentValues values = new ContentValues();
		values.put(PhoneManagerProvider.FREEZE_UID, uid);
		values.put(PhoneManagerProvider.FREEZE_PACKAGE_NAME, info.applicationInfo.packageName);
		contentResolver.insert(uri, values);
		if (!TEST) {
			mAdapter.setApplicationDisable(info.applicationInfo.packageName);
		}
		normalAppInfoList.remove(info);
		info.isFreezed = true;
		freezeAppInfoList.add(info);
	}

	@Override
	public List<FreezeAppInfo> getNormalApps() {
		normalAppInfoList.clear();
		
		FreezePackageManagerAdapter pkgAdapter = FreezePackageManagerAdapter
				.getInstance(mContext);

		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> resolveInfos = pkgAdapter.queryIntentActivities(
				mainIntent, 0);
		Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(
				mContext.getPackageManager()));
		FreezeAppInfo app;
		for (ResolveInfo resolveInfo : resolveInfos) {
			if (!isFilterPackage(resolveInfo.activityInfo.applicationInfo.packageName)) {
				
				app = new FreezeAppInfo();
				app.applicationInfo = resolveInfo.activityInfo.applicationInfo;
				app.name = resolveInfo.activityInfo.applicationInfo
						.loadLabel(mContext.getPackageManager());
				app.icon = resolveInfo.activityInfo.applicationInfo
						.loadIcon(mContext.getPackageManager());
				app.isFreezed = false;
				normalAppInfoList.add(app);
			}
		}
		return normalAppInfoList;
	}
	
	private boolean isFilterPackage(String packageName) {
		
		for (String name : skipPackage) {
			if (packageName.equals(name)) {
				return true;
			}
		}
		
		if (normalAppInfoList != null) {
			for (FreezeAppInfo appInfo : normalAppInfoList) {
				if (appInfo.applicationInfo.packageName.equals(packageName)) {
					return true;
				}
			}
		}
		
		return false;
	}
}
