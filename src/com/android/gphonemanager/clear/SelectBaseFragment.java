package com.android.gphonemanager.clear;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.gphonemanager.R;

public abstract class SelectBaseFragment extends Fragment {
	protected ScanType mData;
	protected Context mContext = getContext();
	protected FragmentListener mListener;
	protected FileIconHelper mFileIconHelper;
	protected long clearSize;
	protected long selectSize;
	protected Thread deleteThread;
	protected static final int MSG_CLEAR_START = 1005;
	protected static final int MSG_CLEAR_COMPLETE = 1006;
	
	protected TextView mSelectAllView;
	protected View mSettingView;
	protected boolean isSelectAll;
	int allCount;
	int selectCount;

	public interface FragmentListener {
		void showProgressBar(boolean show);
		void onClearFinished(boolean success, long size);
		void setCleanButton(String info, boolean isEnable);
	}
	
	public void setListener(FragmentListener listener) {
		this.mListener = listener;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ActionBar actionBar = getActivity().getActionBar();
		if (actionBar != null ) {
			mSelectAllView = (TextView)actionBar.getCustomView().findViewById(R.id.select_all);
			mSettingView = (ImageView)actionBar.getCustomView().findViewById(R.id.settings);
		}
		
	}
	
	public void setData(ScanType data) {
		mData = data;
	}
	
	public void setFileIconHelper(FileIconHelper fileIconHelper){
		this.mFileIconHelper = fileIconHelper;
	}
	
	public abstract void onClearAction();
	
	public void onClearFinished() {
		mData.totalSize -= clearSize;
		mListener.onClearFinished(true, clearSize);
	}
	
	public void selectAll(boolean select) {
	}
	
	public void onSetting() {
	}
	
	private void isSelectAll(boolean select) {
		
		mSelectAllView.setText(select ? getString(R.string.none) : getString(R.string.all));
		mSelectAllView.setTag(select);
		isSelectAll = select;
	}
	
	public void refreshUI() {
		isSelectAll(allCount == selectCount);
		String info;
		if (selectCount == 0) {
			info = getString(R.string.clear_key);
		} else {
			info = getString(R.string.clean_now, Formatter.formatFileSize(mContext, selectSize));
		}
		
		mListener.setCleanButton(info, selectCount == 0 ? false : true);
	}
	
}
