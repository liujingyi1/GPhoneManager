package com.android.gphonemanager.applock;

import java.util.HashMap;
import java.util.List;

public class AppLockPresenter {
	IAppLockView view;
	IAppLockModel model;

	public AppLockPresenter(IAppLockView view, IAppLockModel model) {
		this.view = view;
		this.model = model;
	}

	void getLockApps(List<LockApp> apps) {
		model.getLockApps(apps);
	}
	
	void getLockApps(HashMap<String, LockApp> lockedAppsMap) {
		model.getLockApps(lockedAppsMap);
	}
	
	public void lock(LockApp app, List<LockApp> lockedApps) {
		model.lock(app, lockedApps);
		view.lock(app);
	}
	
	public void lock(LockApp app, HashMap<String, LockApp> lockedAppsMap) {
		model.lock(app, lockedAppsMap);
		view.lock(app);
	}
}
