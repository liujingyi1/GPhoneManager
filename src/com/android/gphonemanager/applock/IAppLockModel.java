package com.android.gphonemanager.applock;

import java.util.HashMap;
import java.util.List;

public interface IAppLockModel {
	void lock(LockApp app, List<LockApp> lockedApps);
	void lock(LockApp app, HashMap<String, LockApp> lockedAppsMap);
	void getLockApps(List<LockApp> apps);
	void getLockApps(HashMap<String, LockApp> lockedAppsMap);
}
