package com.android.gphonemanager.freezeapp;

import java.util.List;

public interface IFreezeModel {
	List<FreezeAppInfo> getFreezeApps();
	List<FreezeAppInfo> getNormalApps();
	void deleteFreezeApp(int uid, FreezeAppInfo info);
	void addFreezeApp(int uid, FreezeAppInfo info);
	
}
