package com.android.gphonemanager.applock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.android.gphonemanager.BaseActivity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.gphonemanager.R;
import com.android.gphonemanager.view.ActivityLoadingView;

public class AppLockActivity extends BaseActivity implements IAppLockView {
	private static final String TAG = "Gmanager.AppLockActivity";

	private static final int MSG_INIT_UI_FINISHED = 1001;

	private static final String[] SKIP_PACKAGES = { "com.rgk.RgkQappsFolder",
			"com.android.fingerprintfeatures", "com.adobe.reader", "com.adobe.flashplayer",
			"com.rgk.theme", "com.android.documentsui","com.android.mhlsettings"};

	ListView mListView;
	TextView topTitle;
	ActivityLoadingView mLoadingView;

	AppLockPresenter presenter;
	AppLockModelImpl model;

	List<LockApp> unlockItems = new ArrayList<>();
	List<LockApp> lockItems = new ArrayList<>();
	private HashMap<Integer, String> packageMap = new HashMap<Integer, String>();
	private LockApp titleItemLock;
	private LockApp titleItemUnLock;
	AppLockAdapter mAdapter;
	PackageManager mPackageManager;
	private Thread initUIThread;

	private Handler H = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_INIT_UI_FINISHED:
				mListView.setAdapter(mAdapter);
				
				topTitle = (TextView) findViewById(R.id.top_title);
				if (lockItems.isEmpty()) {
					topTitle.setText(getString(R.string.title_unlock_app,
							unlockItems.size()));
				} else {
					topTitle.setText(getString(R.string.title_lock_app,
							lockItems.size()));
				}
				mLoadingView.dismiss();

				break;

			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.applock_activity);

		model = new AppLockModelImpl(this);
		presenter = new AppLockPresenter(this, model);

//		presenter.getLockApps(AppLockService.lockedApps);
		presenter.getLockApps(AppLockService.lockedAppsMap);

		mPackageManager = getPackageManager();

		mLoadingView = (ActivityLoadingView) findViewById(R.id.loading_view);
		mListView = (ListView) findViewById(R.id.app_lock_list);

		mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				Log.d(TAG, "firstVisibleItem:" + firstVisibleItem);
				if (!lockItems.isEmpty() && topTitle != null) {
					if (firstVisibleItem > lockItems.size()) {
						topTitle.setText(getString(R.string.title_unlock_app,
								unlockItems.size()));
					} else {
						topTitle.setText(getString(R.string.title_lock_app,
								lockItems.size()));
					}
				}
			}
		});
		
		if(getResources().getBoolean(R.bool.show_app_lock_login_anim)) {
			initUIThread = new Thread(new Runnable() {
	
				@Override
				public void run() {
					initMainUI();
					H.sendEmptyMessage(MSG_INIT_UI_FINISHED);
				}
			});
			initUIThread.start();
		} else {
			initMainUI();
			mLoadingView.setVisibility(View.GONE);
			H.sendEmptyMessage(MSG_INIT_UI_FINISHED);
		}
		setTitle(getString(R.string.app_lock));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (!AppLockService.lockedAppsMap.isEmpty()) {
			Intent intent = new Intent(AppLockActivity.this,
					AppLockService.class);
			intent.putExtra(AppLockService.APP_LOCK_CMD,
					-1);
			startService(intent);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (initUIThread != null) {
			initUIThread.interrupt();
		}
		
	}

	private boolean shouldSkip(String packageName) {
		for (String name : SKIP_PACKAGES) {
			if (name.equals(packageName)) {
				return true;
			}
		}
		return false;
	}

	private void initMainUI() {
		// get all application
		/*
		List<ApplicationInfo> appInfos = mPackageManager
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		Collections.sort(appInfos, new ApplicationInfo.DisplayNameComparator(
				mPackageManager));
		*/

		// get all resolveInfo which has CATEGORY_LAUNCHER
		Intent queryIntent = new Intent(Intent.ACTION_MAIN);
		queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(
				queryIntent, PackageManager.MATCH_ALL);
		Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(
				mPackageManager));

		lockItems.clear();
		unlockItems.clear();
		int i = 0;
		packageMap.clear();
		
		for (ResolveInfo resolveInfo : resolveInfos) {
			if (shouldSkip(resolveInfo.activityInfo.packageName) 
					|| packageMap.containsValue(resolveInfo.activityInfo.packageName)) {
				continue;
			} else {
				packageMap.put(i, resolveInfo.activityInfo.packageName);
				
				LockApp item = new LockApp(resolveInfo.loadLabel(mPackageManager)
						.toString(), resolveInfo.activityInfo.packageName,
						resolveInfo.loadIcon(mPackageManager), false);
				if (AppLockService.lockedAppsMap.containsKey(item.packageName)) {
					item.locked = true;
				}

				if (!item.locked) {
					unlockItems.add(item);
				} else {
					lockItems.add(item);
				}
				
				i++;
			}
		}
		/*
		// put resolveInfos to hashmap
		for (ResolveInfo resolveInfo : resolveInfos) {
			if (shouldSkip(resolveInfo.activityInfo.packageName)) {
				continue;
			} else {
				packageMap.put(i, resolveInfo.activityInfo.packageName);
				i++;
			}
		}

		for (ApplicationInfo info : appInfos) {
			if (packageMap.containsValue(info.packageName)) {
				LockApp item = new LockApp(info.loadLabel(mPackageManager)
						.toString(), info.packageName,
						info.loadIcon(mPackageManager), false);
				if (AppLockService.lockedAppsMap.containsKey(item.packageName)) {
					item.locked = true;
				}

				if (!item.locked) {
					unlockItems.add(item);
				} else {
					lockItems.add(item);
				}
			}
		}
		*/

		Log.d(TAG, "unlockItems: " + unlockItems.size());
		Log.d(TAG, "lockItems: " + lockItems.size());

		initListTitles(unlockItems.size(), lockItems.size());

		mAdapter = new AppLockAdapter(unlockItems, lockItems,
				getLayoutInflater());
	}

	private void initListTitles(int unlockAppCount, int lockAppCount) {
		titleItemLock = new LockApp(null, 0);
		titleItemLock.icon = null;
		titleItemLock.label = getString(R.string.title_lock_app, lockAppCount);
		titleItemLock.locked = false;

		titleItemUnLock = new LockApp(null, 0);
		titleItemUnLock.icon = null;
		titleItemUnLock.label = getString(R.string.title_unlock_app,
				unlockAppCount);
		titleItemUnLock.locked = false;
	}

	private View.OnClickListener clickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			int position = (int) v.getTag();
			Log.d(TAG, "onClick:" + position);
			LockApp item;
			if (position > 0 && position < lockItems.size() + 1) {
				item = lockItems.get(position - 1);
			} else {
				item = unlockItems.get(position
						- (lockItems.isEmpty() ? 0 : (lockItems.size() + 1))
						- 1);
			}

			Switch btn = (Switch) v;
			btn.setChecked(!item.locked);

			presenter.lock(item, AppLockService.lockedAppsMap);

		}
	};

	class AppLockAdapter extends BaseAdapter {

		private List<LockApp> unlockItems;
		private List<LockApp> lockItems;
		private LayoutInflater mInflater;

		public AppLockAdapter(List<LockApp> unlockItems,
				List<LockApp> lockItems, LayoutInflater mInflater) {
			this.unlockItems = unlockItems;
			this.lockItems = lockItems;
			this.mInflater = mInflater;
		}

		@Override
		public int getCount() {
			return (lockItems.isEmpty() ? 0 : (lockItems.size() + 1))
					+ (unlockItems.isEmpty() ? 0 : unlockItems.size() + 1);
		}

		@Override
		public Object getItem(int position) {
			if (position == 0 && !lockItems.isEmpty()) {
				return titleItemLock;
			} else if (position > 0 && position < lockItems.size() + 1) {
				return lockItems.get(position - 1);
			} else if (!lockItems.isEmpty() && position == lockItems.size() + 1
					|| lockItems.isEmpty() && position == 0) {
				return titleItemUnLock;
			} else {
				return unlockItems.get(position
						- (lockItems.isEmpty() ? 0 : (lockItems.size() + 1))
						- 1);
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Log.d(TAG, "getView:" + position);
			ViewHolder mHolder;
			LockApp item = null;
			if (position == 0
					|| (!lockItems.isEmpty() && position == lockItems.size() + 1)) {
				if (lockItems.size() != 0 && position == 0) {
					Log.d(TAG, "Locked");
					item = titleItemLock;
				} else {
					Log.d(TAG, "Unlock");
					item = titleItemUnLock;
				}
			} else {
				if (position > 0 && position < lockItems.size() + 1) {
					item = lockItems.get(position - 1);
				} else {
					item = unlockItems
							.get(position
									- (lockItems.isEmpty() ? 0 : (lockItems
											.size() + 1)) - 1);
				}
			}

			if (convertView == null || convertView.getTag() == null) {
				convertView = mInflater.inflate(R.layout.applock_item, null);
				mHolder = new ViewHolder();
				mHolder.image = (ImageView) convertView
						.findViewById(R.id.image);
				mHolder.label = (TextView) convertView.findViewById(R.id.text);
				mHolder.lock = (Switch) convertView.findViewById(R.id.lock);
				mHolder.title = (TextView) convertView.findViewById(R.id.title);
				convertView.setTag(mHolder);
			} else {
				mHolder = (ViewHolder) convertView.getTag();
			}

			if (item.icon == null) {
				mHolder.image.setVisibility(View.GONE);
				mHolder.lock.setVisibility(View.GONE);
				mHolder.label.setVisibility(View.GONE);
				mHolder.title.setVisibility(View.VISIBLE);

				mHolder.title.setText(item.label);
			} else {
				mHolder.image.setVisibility(View.VISIBLE);
				mHolder.lock.setVisibility(View.VISIBLE);
				mHolder.label.setVisibility(View.VISIBLE);
				mHolder.title.setVisibility(View.GONE);

				mHolder.image.setImageDrawable(item.icon);
				mHolder.label.setText(item.label);
				if (item.locked) {
					mHolder.lock.setChecked(true);
				} else {
					mHolder.lock.setChecked(false);
				}
				mHolder.lock.setTag(position);
				mHolder.lock.setOnClickListener(clickListener);
			}

			return convertView;
		}

		// Google I/O
		class ViewHolder {
			public ImageView image;
			public TextView label;
			public Switch lock;
			public TextView title;
		}
	}

	@Override
	public void lock(LockApp app) {
		Log.d(TAG,
				"lock:" + app.locked + ", " + AppLockService.lockedAppsMap.size());
		if (app.locked) {
			if (AppLockService.lockedAppsMap.size() == 1) {

				Intent intent = new Intent(AppLockActivity.this,
						AppLockService.class);
				intent.putExtra(AppLockService.APP_LOCK_CMD,
						AppLockService.CMD_START_LISTION);
				startService(intent);
			}
		} else {
			if (AppLockService.lockedAppsMap.size() == 0) {
				Intent intent = new Intent(AppLockActivity.this,
						AppLockService.class);
				intent.putExtra(AppLockService.APP_LOCK_CMD,
						AppLockService.CMD_STOP_LISTION);
				startService(intent);
			}
		}
	}

	@Override
	protected void onHomeSelected() {
		super.onHomeSelected();
		finish();
	}
}
