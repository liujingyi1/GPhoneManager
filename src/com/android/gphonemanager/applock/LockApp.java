package com.android.gphonemanager.applock;

import android.graphics.drawable.Drawable;

public class LockApp {
	public enum State {
		LOCKED,
		UNLOCKED,
	}
	
	public String label;
	public Drawable icon;
	public String packageName;
    public boolean locked = false;
	public long startTime;
	public State state = State.LOCKED;
	
	public LockApp(String packageName, long startTime) {
		this.packageName = packageName;
		this.startTime = startTime;
	}
	
	public LockApp(String appLabel, String packageName, Drawable icon) {
		this.label = appLabel;
		this.packageName = packageName;
		this.icon = icon;
	}
	
	public LockApp(String appLabel, String packageName, Drawable icon, boolean isLocked) {
		this.label = appLabel;
		this.packageName = packageName;
		this.icon = icon;
		this.locked = isLocked;
	}
}
