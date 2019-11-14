package com.android.gphonemanager.clear;

public class GroupItem {
	private int nameRes;
	private String size;
	private boolean isChecked;
	private boolean showProgress;
	
	public GroupItem(int nameRes, String size, boolean isChecked, boolean showProgress) {
		this.nameRes = nameRes;
		this.size = size;
		this.isChecked = isChecked;
		this.showProgress = showProgress;
	}
	
	public void setChecked(boolean checked) {
		isChecked = checked;
	}
	
	public void showProgress(boolean show) {
		showProgress = show;
	}
}
