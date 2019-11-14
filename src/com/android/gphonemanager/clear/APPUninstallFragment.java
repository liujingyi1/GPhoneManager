package com.android.gphonemanager.clear;


import android.R.bool;
import android.app.ActionBar;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter.AllCaps;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.Inflater;

import com.android.gphonemanager.R;


import android.content.pm.IPackageDeleteObserver;

public class APPUninstallFragment extends SelectBaseFragment{
	
	List<APPInfo> mNoUseApps = new ArrayList<APPInfo>();
	List<APPInfo> mNormalApps = new ArrayList<APPInfo>();
	List<UsageStats> mUsageList;
	ListView mListView;
	
	AppAdapter mAppAdapter;
	private static final int MSG_INIT_UI_FINISHED = 2001;
	
	private static final int MSG_START_DELETE = 2002;
	private static final int MSG_DELETE_COMPLETE = 2003;
	
	List<String> deleteApp = new ArrayList<String>();
	View mRootView;
	
	public APPUninstallFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.appuninstall_fragment_layout, container, false);
		
		return mRootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mContext = getContext();
		
		mSelectAllView.setVisibility(View.VISIBLE);
		
		configureData();
		
		mListView = (ListView)mRootView.findViewById(R.id.app_list);
		
		mAppAdapter = new AppAdapter(mNoUseApps, mNormalApps, getActivity().getLayoutInflater());
		mListView.setAdapter(mAppAdapter);
		mListView.setOnItemClickListener(mAppAdapter);
	}
	
	private Handler H = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_INIT_UI_FINISHED:
//				mListView.setAdapter(mAdapter);
//				
//				topTitle = (TextView) findViewById(R.id.top_title);
//				if (lockItems.isEmpty()) {
//					topTitle.setText(getString(R.string.title_unlock_app,
//							unlockItems.size()));
//				} else {
//					topTitle.setText(getString(R.string.title_lock_app,
//							lockItems.size()));
//				}
			case MSG_START_DELETE:{
				mListener.showProgressBar(true);
				break;
			}
			case MSG_DELETE_COMPLETE:{
				onClearFinished();
//				for (int i = 0; i < mNoUseApps.size(); i++) {
//					if (mNoUseApps.get(i).isCheck) {
//						//mNoUseApps
//					}
//				}
				
				break;
			}
			default:
				break;
			}
		};
	};
	
	private void deleteApps() {
		deleteThread = new Thread(new Runnable() {
			IPackageDeleteObserver.Stub  mDeleteObserver = new IPackageDeleteObserver.Stub() {
				public void packageDeleted(String packageName, int returnCode) {
					
					for (int i = 0; i < deleteApp.size(); i++) {
						if (packageName.equals(deleteApp.get(i))) {
							deleteApp.remove(i);
						}
					}
					
					for (int i = 0; i < mNoUseApps.size(); i++) {
						if (mNoUseApps.get(i).mPackageName.equals(packageName)) {
							clearSize += mNoUseApps.get(i).mSize;
							mNoUseApps.get(i).isDelete = true;
						}
					}
					
					for (int i = 0; i < mNormalApps.size(); i++) {
						if (mNormalApps.get(i).mPackageName.equals(packageName)) {
							clearSize += mNormalApps.get(i).mSize;
							mNormalApps.get(i).isDelete = true;
						}
					}
					
					if (deleteApp.size() == 0) {
						Message msg =  H.obtainMessage(MSG_DELETE_COMPLETE);
						H.sendMessage(msg);
					}
					
				}
			};
			@Override
			public void run() {
				
				try {
					Message msg =  H.obtainMessage(MSG_START_DELETE);
					H.sendMessage(msg);
					PackageManager packageManager = getActivity().getPackageManager();
	                Method deletePackage = packageManager.getClass().getMethod(
	                        "deletePackage",
	                        new Class[] { String.class, IPackageDeleteObserver.class, int.class});
	                for (String name : deleteApp) {
	                	deletePackage.invoke(packageManager,
	                            new Object[] { name, mDeleteObserver, 0});
					} 
				} catch (Exception e) {
					// TODO: handle exception
				}
				
			}
		});
		deleteThread.start();
	}
	
	
	private void configureData() {
		
		mNoUseApps.clear();
		mNormalApps.clear();
		selectSize = 0;
		selectCount = 0;
		allCount = 0;
		
		UsageStatsManager usageStatsManager = 
				(UsageStatsManager)getContext().getSystemService(Context.USAGE_STATS_SERVICE);
		
		long current = System.currentTimeMillis();
 		Calendar calendar = Calendar.getInstance();
 		calendar.setTimeInMillis(current);
 		int year = calendar.get(Calendar.YEAR);
 		
 		calendar.add(Calendar.YEAR, -3);
		
 		if (mUsageList == null) {
			mUsageList = usageStatsManager.queryUsageStats(
					UsageStatsManager.INTERVAL_BEST, calendar.getTimeInMillis(), current);
 		}
		
		long size = 0;
		for (APPInfo appInfo : mData.uninstallApp.mAppInfoList) {
			String packageNmae = appInfo.mPackageName;
			
			if (!appInfo.isDelete) {
				if (isNoUseApp(packageNmae)) {
					mNoUseApps.add(appInfo);
					allCount++;
				} else {
					mNormalApps.add(appInfo);
					allCount++;
				}
				if (appInfo.isCheck) {
					selectSize += appInfo.mSize;
					selectCount++;
				}
				size += appInfo.mSize;
			}
		}
		
		mData.uninstallApp.size = size;
		if (mAppAdapter != null) {
			mAppAdapter.notifyDataSetChanged();
		}
		refreshUI();
	}
	
	private void onListItemClick(int position) {
		boolean check;
		if (position < mNoUseApps.size()+1) {
			check = mNoUseApps.get(position-1).isCheck;
			mNoUseApps.get(position-1).isCheck = !check;
			selectCount += !check ? 1 : -1;
			selectSize += !check ? mNoUseApps.get(position-1).mSize : -mNoUseApps.get(position-1).mSize;
		} else {
			int perCount = mNoUseApps.size() > 0 ? mNoUseApps.size()+1 : 0;
			check = mNormalApps.get(position- perCount -1).isCheck;
			mNormalApps.get(position- perCount -1).isCheck = !check;
			selectCount += !check ? 1 : -1;
			selectSize += !check ? mNormalApps.get(position- perCount -1).mSize : -mNormalApps.get(position- perCount -1).mSize;
		}
		
		refreshUI();
		
		mAppAdapter.notifyDataSetChanged();
	}
	
	private class AppAdapter extends BaseAdapter implements AdapterView.OnItemClickListener{
		List<APPInfo> nouseApps;
		List<APPInfo> normalApps;
		LayoutInflater mInflater;
		
		public AppAdapter(List<APPInfo> nouseapps, List<APPInfo> normalapps, LayoutInflater inflater) {
			this.nouseApps = nouseapps;
		    this.normalApps = normalapps;
			mInflater = inflater;
		}

		@Override
		public int getCount() {
			return (nouseApps.isEmpty() ? 0 : (nouseApps.size() + 1))
					+ (normalApps.isEmpty() ? 0 : normalApps.size() + 1);
		}

		@Override
		public Object getItem(int position) {
			if (position == 0 && !nouseApps.isEmpty()) {
				return null;
			} else if (position > 0 && position < nouseApps.size() + 1) {
				return nouseApps.get(position - 1);
			} else if (!nouseApps.isEmpty() && position == nouseApps.size() + 1
					|| nouseApps.isEmpty() && position == 0) {
				return null;
			} else {
				return normalApps.get(position
						- (nouseApps.isEmpty() ? 0 : (nouseApps.size() + 1))
						- 1);
			}
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder mHolder;
			APPInfo item = null;
			boolean check = false;
			
			item = (APPInfo)getItem(position);
			if (item == null) {
				convertView = mInflater.inflate(R.layout.app_select_title_item_view, null);
				TextView textView = (TextView)convertView.findViewById(R.id.title);
				if (nouseApps.size() != 0 && position == 0) {
					textView.setText(R.string.not_frequent_apps);
				} else {
					textView.setText(R.string.frequent_apps);
				}
				return convertView;
			}
			
			if (convertView == null || convertView.getTag() == null) {
				convertView = mInflater.inflate(R.layout.app_select_item, null);
				mHolder = new ViewHolder();
				mHolder.image = (ImageView) convertView.findViewById(R.id.image);
				mHolder.label = (TextView) convertView.findViewById(R.id.label);
				mHolder.size = (TextView) convertView.findViewById(R.id.size);
				mHolder.check = (CheckBox) convertView.findViewById(R.id.checkbox);
				convertView.setTag(mHolder);
			} else {
				mHolder = (ViewHolder) convertView.getTag();
			}
			
			View view = (View)convertView.findViewById(R.id.divider_line);
			if (position == nouseApps.size() || position == (getCount()-1)) {
				view.setVisibility(View.GONE);
			} else {
				view.setVisibility(View.VISIBLE);
			}
			
			mHolder.image.setImageDrawable(item.iconBitmap);
			mHolder.label.setText(item.label);
			mHolder.size.setText(item.mSizeStr);
			mHolder.check.setChecked(item.isCheck);
			
			return convertView;
		}
		
		@Override
		public boolean isEnabled(int position) {
			return !(position == 0
					|| (!nouseApps.isEmpty() && position == nouseApps.size() + 1));
		}
		
		class ViewHolder {
			public ImageView image;
			public TextView label;
			public TextView summary;
			public TextView size;
			public CheckBox check;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			onListItemClick(position);
		}
	}
	
	private boolean isNoUseApp(String packageName) {
		for (UsageStats usageStats : mUsageList) {
			if (packageName.equals(usageStats.getPackageName())) {
				try {
//					Field field = usageStats.getClass().getDeclaredField("mLaunchCount");
//					field.setAccessible(true);
//					int count = field.getInt(usageStats);
					
					long current = System.currentTimeMillis();
			 		Calendar calendar = Calendar.getInstance();
			 		calendar.setTimeInMillis(current - 1000 * 60 * 60 *24 * 7);
			 		
//			 		calendar.add(Calendar.DAY_OF_MONTH, -1);
					
					if (calendar.getTimeInMillis() >= usageStats.getLastTimeUsed()) {
						return true;
					}
					
				} catch (Exception e) {
					
				}
				break;
			}
		}
		
		return false;
	}
	
	@Override
	public void onClearAction() {
		deleteApp.clear();
		clearSize = 0;
		for (APPInfo appInfo : mNoUseApps) {
			if (appInfo.isCheck) {
				deleteApp.add(appInfo.mPackageName);
			}
		}
		for (APPInfo appInfo : mNormalApps) {
			if (appInfo.isCheck) {
				deleteApp.add(appInfo.mPackageName);
			}
		}
		deleteApps();
	}

	
	@Override
	public void onClearFinished() {
		configureData();
		mAppAdapter.notifyDataSetChanged();
		super.onClearFinished();
	}
	
	public void selectAll(boolean select) {
		selectSize = 0;
		for (APPInfo appInfo : mNoUseApps) {
			appInfo.isCheck = select;
			selectSize += appInfo.mSize;
		}
		for (APPInfo appInfo : mNormalApps) {
			appInfo.isCheck = select;
			selectSize += appInfo.mSize;
		}
		
		selectCount = select ? allCount : 0;
		selectSize = select ? selectSize : 0;
		
		refreshUI();
		if (mAppAdapter != null) {
		    mAppAdapter.notifyDataSetChanged();
		}
	}

}
