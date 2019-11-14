package com.android.gphonemanager.netmanager;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.ActivityManager.MemoryInfo;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gphonemanager.R;
import com.android.gphonemanager.netmanager.SubInfoUtils;
import com.android.gphonemanager.netmanager.Constant;

public class FireWallFragment extends Fragment {
	Context mContext;
	
	ListView mAppListView;
	AppItemAdapter mAppItemAdapter = null;
	View mProgressBar;
	CheckBox mMobileCheckAll;
	CheckBox mWlanCheckAll;
	View mMobileCheckLayout;
	View mWlanCheckLayout;
	View mTopView;
	
	FirewallDatabaseHelper mFirewallDatabaseHelper = null;
	NetAdapter mFirewallService = null;
	boolean mOnlyUserApp = false;

	
	public final LoaderCallbacks<ArrayList<AppUsage>> mAppsLoaderCallbacks = new LoaderCallbacks<
			ArrayList<AppUsage>>() {
        @Override
        public Loader<ArrayList<AppUsage>> onCreateLoader(int id, Bundle args) {
            AppsLoader loader = new AppsLoader(getActivity());
            loader.setExtras(args);
            
            showProcessBar(true);
            return loader;
        }

		@Override
		public void onLoadFinished(Loader<ArrayList<AppUsage>> loader,
				ArrayList<AppUsage> appDetails) {
			
			changeData(appDetails);
			refleshUI();

			showProcessBar(false);
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<AppUsage>> arg0) {
			// TODO Auto-generated method stub
			
		}
    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFirewallDatabaseHelper = new FirewallDatabaseHelper(getActivity());
		mFirewallService = NetAdapter.getInstance(this.getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getContext();

		View view = inflater.inflate(R.layout.net_control_fragment, null);
		mAppListView = (ListView) view.findViewById(R.id.listview_app);
		mProgressBar = view.findViewById(R.id.progressBar);
		mProgressBar.setOnClickListener(mOnClickListener);
		
		mTopView = view.findViewById(R.id.top);
		mTopView.setOnClickListener(mOnClickListener);
		
		mMobileCheckAll = (CheckBox)view.findViewById(R.id.select_all_mobile);
		mWlanCheckAll = (CheckBox)view.findViewById(R.id.select_all_wlan);
		mMobileCheckAll.setOnClickListener(mOnClickListener);
		mWlanCheckAll.setOnClickListener(mOnClickListener);
		
		mMobileCheckLayout = view.findViewById(R.id.all_mobile_container);;
		mWlanCheckLayout = view.findViewById(R.id.all_wlan_container);;
		
		mAppItemAdapter = new AppItemAdapter();
		mAppListView.setAdapter(mAppItemAdapter);
		
		return view;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		reLoad();
	}
	
	public void showProcessBar(boolean show) {
		if (mProgressBar != null) {
			if (show) {
				mProgressBar.setVisibility(View.VISIBLE);
				mMobileCheckLayout.setVisibility(View.GONE);
				mWlanCheckLayout.setVisibility(View.GONE);
			} else {
				mProgressBar.setVisibility(View.GONE);
				mMobileCheckLayout.setVisibility(View.VISIBLE);
				mWlanCheckLayout.setVisibility(View.VISIBLE);
			}
		}
	}
	
    public void changeData(ArrayList<AppUsage> data) {
    	mAppItemAdapter.changeData(data);
    }
    
    public void refleshUI() {
		if (mAppItemAdapter != null) {
			
			int mobilecount = mAppItemAdapter.getCount();
			int mobileDenyCount = mFirewallDatabaseHelper.getMobileUidCount();
			mMobileCheckAll.setChecked(mobileDenyCount==0);
			
			int wlancount = mAppItemAdapter.getCount();
			int wlanDenyCount = mFirewallDatabaseHelper.getWifiUidCount();
			mWlanCheckAll.setChecked(wlanDenyCount==0);
			
			mAppItemAdapter.notifyDataSetChanged();
		}
    }
	
	public void reLoad() {
		
        Bundle extras = new Bundle();
        extras.putBoolean("only_user_app", mOnlyUserApp);
		
		getLoaderManager().restartLoader(Constant.LOADER_ID_FOR_FIREWALL, extras, mAppsLoaderCallbacks);
	}
	
	private void checkMobileAll(boolean check) {
		CheckAllTask task = new CheckAllTask();
		// arg1:isMobile, arg2:isCheck
		task.execute(true, check);
	}
	
	private class CheckAllTask extends AsyncTask<Boolean, Object, Void> {

		@Override
		protected void onPreExecute() {
			mProgressBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected Void doInBackground(Boolean... params) {
			boolean isMobile = params[0];
			boolean isCheck = params[1];
			
			if (mAppItemAdapter != null) {
				ArrayList<AppUsage> appUsages = mAppItemAdapter.getData();
					for (AppUsage appUsage : appUsages) {
						if (!isCheck) {
							if (isMobile) {
								if (!appUsage.selected_mobile) {
									boolean result = false;
									try {
									    mFirewallService.setFirewallUidChainRule(appUsage.uid, 
											AppUtils.NETWORK_TYPE_MOBILE, true);
									    result = true;
									} catch (NetRemoteException e) {
										result = false;
										e.printStackTrace();
									}
									if (result) {
										appUsage.selected_mobile = true;
										mFirewallDatabaseHelper.insertDenyUidWithMobile(appUsage.uid);
									}
								}
							} else {
								if (!appUsage.selected_wifi) {
									boolean result = false;
									try {
									mFirewallService.setFirewallUidChainRule(appUsage.uid, 
											AppUtils.NETWORK_TYPE_WIFI, true);
									result = true;
									} catch (NetRemoteException e) {
										result = false;
										e.printStackTrace();
									}
									if (result) {
										appUsage.selected_wifi = true;
										mFirewallDatabaseHelper.insertDenyUidWithWifi(appUsage.uid);
									}
								}
							}
						} else {
							if (isMobile) {
								if (appUsage.selected_mobile) {
									boolean result = false;
									try {
									    mFirewallService.setFirewallUidChainRule(appUsage.uid, 
											    AppUtils.NETWORK_TYPE_MOBILE, false);
									    result = true;
									} catch (NetRemoteException e) {
										result = false;
										e.printStackTrace();
									}
									if (result) {
									    appUsage.selected_mobile = false;
									    mFirewallDatabaseHelper.deleteDenyUidWithMobile(appUsage.uid);
									}
								}
							} else {
								if (appUsage.selected_wifi) {
									boolean result = false;
									try {
									    mFirewallService.setFirewallUidChainRule(appUsage.uid, 
											    AppUtils.NETWORK_TYPE_WIFI, false);
								        result = true;
									} catch (NetRemoteException e) {
										result = false;
										e.printStackTrace();
									}
									if (result) {
										appUsage.selected_wifi = false;
										mFirewallDatabaseHelper.deleteDenyUidWithWifi(appUsage.uid);
									}
								}
							}
						}
					}
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			refleshUI();
			mProgressBar.setVisibility(View.GONE);
		}
		
	}
	
	private void checkWlanAll(boolean check) {
		CheckAllTask task = new CheckAllTask();
		// arg1:isMobile, arg2:isCheck
		task.execute(false, check);
	}
	
	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			AppUsage appDetail = (AppUsage)view.getTag();
			switch (view.getId()) {
				case R.id.check_mobile: {
					boolean mobileResult = false;
					try {
						mFirewallService.setFirewallUidChainRule(appDetail.uid, 
								AppUtils.NETWORK_TYPE_MOBILE, !appDetail.selected_mobile);
						mobileResult = true;
					} catch (NetRemoteException e) {
						mobileResult = false;
						e.printStackTrace();
					}
					if (!mobileResult) {
						Toast.makeText(getActivity(), R.string.set_net_able_failed, 1000).show();
						return;
					}
					
					appDetail.selected_mobile = !appDetail.selected_mobile;
					CheckBox enable = (CheckBox)view;
					if (appDetail.selected_mobile) {
						enable.setChecked(!appDetail.selected_mobile);
						mFirewallDatabaseHelper.insertDenyUidWithMobile(appDetail.uid);
					} else {
						enable.setChecked(!appDetail.selected_mobile);
						mFirewallDatabaseHelper.deleteDenyUidWithMobile(appDetail.uid);
					}
					
					refleshUI();
					
					Toast.makeText(getActivity(), R.string.set_net_able_success, 1000).show();
					break;
				}
				case R.id.check_wlan: {
					boolean wifiResult = false;
					try {
						mFirewallService.setFirewallUidChainRule(appDetail.uid, 
								AppUtils.NETWORK_TYPE_WIFI, !appDetail.selected_wifi);
						wifiResult = true;
					} catch (NetRemoteException e) {
						wifiResult = false;
						e.printStackTrace();
					}
					if (!wifiResult) {
						Toast.makeText(getActivity(), R.string.set_net_able_failed, 1000).show();
						return;
					}
					
					appDetail.selected_wifi = !appDetail.selected_wifi;
					CheckBox enable = (CheckBox)view;
					if (appDetail.selected_wifi) {
						enable.setChecked(!appDetail.selected_wifi);
						mFirewallDatabaseHelper.insertDenyUidWithWifi(appDetail.uid);
					} else {
						enable.setChecked(!appDetail.selected_wifi);
						mFirewallDatabaseHelper.deleteDenyUidWithWifi(appDetail.uid);
					}
					
					refleshUI();
					
					Toast.makeText(getActivity(), R.string.set_net_able_success, 1000).show();
					break;
				}
				case R.id.select_all_mobile: {
					CheckBox checkBox = (CheckBox)view;
					checkMobileAll(checkBox.isChecked());
					break;
				}
				
				case R.id.select_all_wlan: {
					CheckBox checkBox = (CheckBox)view;
					checkWlanAll(checkBox.isChecked());
					break;
				}
				
				case R.id.top: {
					doubleClick();
					break;
				}
			}
		}
	};
	private long[] mHits = new long[2];
	private void doubleClick() {
		System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
		mHits[mHits.length-1] = SystemClock.uptimeMillis();
		if (mHits[mHits.length-1] - mHits[0] < 500) {
			if (mAppItemAdapter != null) {
				mAppListView.smoothScrollToPosition(0);
			}
		}
	}
	
	class AppItemAdapter extends BaseAdapter {
		
		private ArrayList<AppUsage> appUsages = null;
		private SubInfoUtils mSubInfoUtils;
		boolean sim1Enable = false;
		boolean sim2Enable = false;
		
		public AppItemAdapter() {
			mSubInfoUtils = SubInfoUtils.getInstance(mContext);
			appUsages = new ArrayList<AppUsage>();
			
			int[] slot = mSubInfoUtils.getActivatedSlotList();
			for (int i : slot) {
				sim1Enable = (i == 0) ? true : sim1Enable;
				sim2Enable = (i == 1) ? true : sim2Enable;
			}
		}
		
		public ArrayList<AppUsage> getData() {
			return appUsages;
		}
		
		@Override
		public int getCount() {
			if (appUsages != null) {
				return appUsages.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return appUsages.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(getActivity());
			ViewHolder viewHolder = null;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.net_control_item_view2, null);
				viewHolder = new ViewHolder();
				viewHolder.icon = (ImageView)convertView.findViewById(R.id.image);
				viewHolder.appName = (TextView)convertView.findViewById(R.id.label);
				viewHolder.mobileContainer = convertView.findViewById(R.id.mobile_usage);
				viewHolder.wifiContainer = convertView.findViewById(R.id.wifi_usage);
				viewHolder.sim1Datausage = (TextView) convertView.findViewById(R.id.sim1_data);
				viewHolder.sim2Datausage = (TextView) convertView.findViewById(R.id.sim2_data);
				viewHolder.wifiDatausage = (TextView) convertView.findViewById(R.id.wifi_data);
				viewHolder.checkMobile = (CheckBox) convertView.findViewById(R.id.check_mobile);
				viewHolder.checkWlan = (CheckBox) convertView.findViewById(R.id.check_wlan);
				viewHolder.checkMobile.setOnClickListener(mOnClickListener);
				viewHolder.checkWlan.setOnClickListener(mOnClickListener);
				viewHolder.mobileContainer.setVisibility(View.VISIBLE);
				viewHolder.wifiContainer.setVisibility(View.VISIBLE);
				if (sim1Enable) {
					((View)convertView.findViewById(R.id.sim_1)).setVisibility(View.VISIBLE);
				}
				if (sim2Enable) {
					((View)convertView.findViewById(R.id.sim_2)).setVisibility(View.VISIBLE);
				}
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
			AppUsage appUsage = appUsages.get(position);
			viewHolder.checkMobile.setTag(appUsage);
			viewHolder.checkWlan.setTag(appUsage);
			viewHolder.icon.setImageDrawable(appUsage.icon);
			viewHolder.appName.setText(appUsage.name);
			
			if (sim1Enable) {
				viewHolder.sim1Datausage.setText(Formatter.formatFileSize(mContext, appUsage.sim1Total));
			}
			if (sim2Enable) {
				viewHolder.sim2Datausage.setText(Formatter.formatFileSize(mContext, appUsage.sim2Total));
			}
			
			viewHolder.wifiDatausage.setText("WLAN: "+Formatter.formatFileSize(mContext, appUsage.wifiTotal));
			
			boolean enabled = SharePreferenceUtils.getEnabled(getActivity());
			boolean checkmobile = (!appUsage.selected_mobile || enabled);
			boolean checkwlan = (!appUsage.selected_wifi || enabled);
			viewHolder.checkMobile.setChecked(checkmobile);
			viewHolder.checkWlan.setChecked(checkwlan);

			if (enabled) {
				viewHolder.checkMobile.setEnabled(false);
				viewHolder.checkWlan.setEnabled(false);
			} else {
				viewHolder.checkMobile.setEnabled(true);
				viewHolder.checkWlan.setEnabled(true);
			}
			
			return convertView;
		}
		
		public void changeData(ArrayList<AppUsage> appDetails){
			this.appUsages = appDetails;
			this.notifyDataSetChanged();
		}
	}

	
	class ViewHolder {
		ImageView icon;
		TextView appName;
		View mobileContainer;
		View wifiContainer;
		TextView sim1Datausage;
		TextView sim2Datausage;
		TextView wifiDatausage;
		CheckBox checkMobile;
		CheckBox checkWlan;
	}
	
}
