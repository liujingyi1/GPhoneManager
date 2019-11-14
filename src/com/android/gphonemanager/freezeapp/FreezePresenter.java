package com.android.gphonemanager.freezeapp;

import java.util.List;

public class FreezePresenter {
	IFreezeView view;
	IFreezeModel model;
	
	public FreezePresenter (IFreezeView view, IFreezeModel model) {
		this.view = view;
		this.model = model;
	}
	
	public List<FreezeAppInfo> getFreezeApps() {
		return model.getFreezeApps();
	}
	
	public List<FreezeAppInfo> getNormalApps() {
		return model.getNormalApps();
	}
	
	public void deleteFreezeApp(int uid, FreezeAppInfo mAppInfo) {
		model.deleteFreezeApp(uid, mAppInfo);
		view.deleteFreezeApp(mAppInfo);
	}

	public void addFreezeApp(int uid, FreezeAppInfo mAppInfo) {
		model.addFreezeApp(uid, mAppInfo);
		view.addFreezeApp(mAppInfo);
	}
}
