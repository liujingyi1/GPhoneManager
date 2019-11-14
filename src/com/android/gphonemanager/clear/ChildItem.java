package com.android.gphonemanager.clear;

import android.graphics.drawable.Drawable;

public class ChildItem {
	private Drawable icon;
	private String name;
	private String size;
	private boolean isChecked;
	
	public ChildItem(Drawable icon, String name, String size, boolean isChecked) {
		this.icon = icon;
		this.name = name;
		this.size = size;
		this.isChecked = isChecked;
	}
	
	public void setChecked(boolean checked) {
		isChecked = checked;
	}
}
