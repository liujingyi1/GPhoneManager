package com.android.gphonemanager.netmanager;

import java.util.ArrayList;

import com.android.gphonemanager.netmanager.AppStateDataUsageBridge.DataUsageState;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.gphonemanager.R;

public class BackgroundFragment extends Fragment 
             implements ApplicationsState.Callbacks, AppStateBaseBridge.Callback{
	private AppItemAdapter mAppItemAdapter = null;
	
	Context mContext;
	
	ListView mAppListView;
	
	View mProgressBar;
	
    private ApplicationsState mApplicationsState;
    private AppStateDataUsageBridge mDataUsageBridge;
    private ApplicationsState.Session mSession;
    private DataSaverBackend mDataSaverBackend;
    private boolean mExtraLoaded;
    private AppFilter mFilter;
	
    NetAdapter mFirewallService = null;
    CheckBox mCheckAll;
    View mTopView;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mFirewallService = NetAdapter.getInstance(this.getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getContext();

		View view = inflater.inflate(R.layout.net_control_fragment, null);
		mAppListView = (ListView) view.findViewById(R.id.listview_app);
		mProgressBar = view.findViewById(R.id.progressBar);
		mProgressBar.setOnClickListener(mOnClickListener);
		
		mAppItemAdapter = new AppItemAdapter();
		mAppListView.setAdapter(mAppItemAdapter);
		
		mTopView = view.findViewById(R.id.top);
		mTopView.setOnClickListener(mOnClickListener);
		
		View fireWallContainer = view.findViewById(R.id.all_mobile_container);
		fireWallContainer.setVisibility(View.GONE);
		mCheckAll = (CheckBox)view.findViewById(R.id.select_all_wlan);
		mCheckAll.setOnClickListener(mOnClickListener);
		View textview = view.findViewById(R.id.wlan_text);
		textview.setVisibility(View.GONE);
		
        mApplicationsState = ApplicationsState.getInstance(
                (Application) getContext().getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, 
        		mDataSaverBackend, mFirewallService, mContext);
        mSession = mApplicationsState.newSession(this);
        mFilter = ApplicationsState.FILTER_THIRD_PARTY;
		
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mProgressBar.setVisibility(View.VISIBLE);
		mCheckAll.setVisibility(View.GONE);
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		MetricsLogger.visible(getActivity(), getMetricsCategory());
        mSession.resume();
        mDataUsageBridge.resume();
        mDataSaverBackend.addListener(mAppItemAdapter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		MetricsLogger.hidden(getActivity(), getMetricsCategory());
		
        mDataSaverBackend.remListener(mAppItemAdapter);
        mDataUsageBridge.pause();
        mSession.pause();
       
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
        mSession.release();
        mDataUsageBridge.release();
	}
	
    protected int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_UNRESTRICTED_ACCESS;
    }

    @Override
    public void onExtraInfoUpdated() {
        mExtraLoaded = true;
        rebuild();
    }
    
    private void rebuild() {
        ArrayList<AppEntry> apps = mSession.rebuild(mFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }
    
    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (getContext() == null) return;
        
//        Collections.sort(apps);
        
        ArrayList<AppEntry> appEntries = new ArrayList<AppEntry>();
        for (AppEntry appEntry : apps) {
			if (appEntry.info.uid >= 10000) {
				appEntries.add(appEntry);
			}
		}
        
        changeData(appEntries);
        refreshUI();
        mProgressBar.setVisibility(View.GONE);
        mCheckAll.setVisibility(View.VISIBLE);
    }
    
    private void refreshUI() {
    	if (mAppItemAdapter != null) {
    		ArrayList<AppEntry> apps = mAppItemAdapter.getData();
    		boolean allNotBlack = true;
    		for (AppEntry appEntry : apps) {
    		    DataUsageState mState;
    		    appEntry.ensureLabel(getContext());
                mState = (DataUsageState) appEntry.extraInfo;
                if (mState != null) {
	                if (mState.isDataSaverBlacklisted) {
	                	allNotBlack = false;
	                	break;
	                }
                }
			}
    		mCheckAll.setChecked(allNotBlack);
    		
    		mAppItemAdapter.notifyDataSetChanged();
    	}
    }
	
    public void changeData(ArrayList<AppEntry> data) {
    	mAppItemAdapter.changeData(data);
    }
    
	class AppItemAdapter extends BaseAdapter implements DataSaverBackend.Listener {
		
		private ArrayList<AppEntry> appUsages = null;
		
		public AppItemAdapter() {
			appUsages = new ArrayList<AppEntry>();
		}
		
		public ArrayList<AppEntry> getData() {
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
				viewHolder.wifiContainer = convertView.findViewById(R.id.wifi_usage);
				viewHolder.mobileContainer = convertView.findViewById(R.id.mobile_usage);
				viewHolder.mobileDataContainer = convertView.findViewById(R.id.mobile_data_container);
				viewHolder.mobileDatausage = (TextView) convertView.findViewById(R.id.mobile_data);
				viewHolder.wifiDatausage = (TextView) convertView.findViewById(R.id.wifi_data);
				viewHolder.sim1Datausage = (TextView) convertView.findViewById(R.id.sim1_data);
				viewHolder.sim2Datausage = (TextView) convertView.findViewById(R.id.sim2_data);
				viewHolder.enable = (CheckBox) convertView.findViewById(R.id.check_wlan);
				viewHolder.enable.setOnClickListener(mOnClickListener);
				View mobileCheck = (CheckBox) convertView.findViewById(R.id.check_mobile);
				mobileCheck.setVisibility(View.GONE);
				
				viewHolder.wifiContainer.setVisibility(View.VISIBLE);
				viewHolder.mobileContainer.setVisibility(View.VISIBLE);
				viewHolder.mobileDataContainer.setVisibility(View.VISIBLE);
				
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
		    DataUsageState mState;
			AppEntry entry = appUsages.get(position);
			entry.ensureLabel(getContext());
            mState = (DataUsageState) entry.extraInfo;
			
			viewHolder.enable.setTag(entry);
			viewHolder.icon.setImageDrawable(entry.icon);
			viewHolder.appName.setText(entry.label);
			String mobileLabel = getString(R.string.mobile_tab_label);
			String wifiLabel = getString(R.string.wifi_tab_label);
			viewHolder.wifiDatausage.setText(mobileLabel+":"+
			                         Formatter.formatFileSize(mContext, mState.totalBackData));
			viewHolder.mobileDatausage.setText(wifiLabel+":"+
			                         Formatter.formatFileSize(mContext, mState.totalBackData));
			
			boolean enabled = SharePreferenceUtils.getEnabled(getActivity());
			boolean check = (!mState.isDataSaverBlacklisted || enabled);
			viewHolder.enable.setChecked(check);

			if (enabled) {
				viewHolder.enable.setEnabled(false);
			} else {
				viewHolder.enable.setEnabled(true);
			}
			
			return convertView;
		}
		
		public void changeData(ArrayList<AppEntry> appDetails){
			this.appUsages = appDetails;
			this.notifyDataSetChanged();
		}
		
        @Override
        public void onDataSaverChanged(boolean isDataSaving) {
        }

        @Override
        public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        }

        @Override
        public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        	
        	Log.i("jingyi", "uid="+uid+" isBlacklisted="+isBlacklisted);
        	
        	for (AppEntry appEntry : appUsages) {
				if (appEntry.info.uid == uid) {
					DataUsageState state = (DataUsageState)appEntry.extraInfo;
					state.isDataSaverBlacklisted = isBlacklisted;
					refreshUI();
				}
			}
        }
	}
	
	private void checkAll(boolean check) {
		if (mAppItemAdapter != null) {
			ArrayList<AppEntry> data = mAppItemAdapter.getData();
			for (AppEntry appEntry : data) {
    		    DataUsageState mState;
    		    appEntry.ensureLabel(getContext());
                mState = (DataUsageState) appEntry.extraInfo;
                if (!check) {
                    if (!mState.isDataSaverBlacklisted) {
                        mDataSaverBackend.setIsBlacklisted(appEntry.info.uid,
                        		appEntry.info.packageName, true);
                    }	
                } else {
                    if (mState.isDataSaverBlacklisted) {
                        mDataSaverBackend.setIsBlacklisted(appEntry.info.uid,
                        		appEntry.info.packageName, false);
                    }
				}
			}
			mAppItemAdapter.notifyDataSetChanged();
		}
	}
	
	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			Log.d("firewall", "[onClick] " + view);
			
			AppEntry entry = (AppEntry)view.getTag();

            switch (view.getId()) {
            	case R.id.check_wlan: {
            		boolean isBacklist = mDataSaverBackend.isBlacklisted(entry.info.uid);
                    mDataSaverBackend.setIsBlacklisted(entry.info.uid,
                    		entry.info.packageName, !isBacklist);
                    
//                    DataUsageState state = (DataUsageState) entry.extraInfo;
//                    state.isDataSaverBlacklisted = !isBacklist;
//                    CheckBox enable = (CheckBox)view;
//					enable.setChecked(state.isDataSaverBlacklisted);
					
					Toast.makeText(getActivity(), R.string.set_net_able_success, 1000).show();
					
					refreshUI();
                    
            		break;
            	}
            	case R.id.select_all_wlan: {
            		CheckBox allCheck = (CheckBox)view;
            		checkAll(allCheck.isChecked());
            	}
				case R.id.top: {
					doubleClick();
					break;
				}
            	default:
            	    break;
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
	
	class ViewHolder {
		ImageView icon;
		TextView appName;
		View mobileContainer;
		View wifiContainer;
		View mobileDataContainer;
		TextView sim1Datausage;
		TextView sim2Datausage;
		TextView wifiDatausage;
		TextView mobileDatausage;
		CheckBox enable;
	}
	
    @Override
    public void onPackageIconChanged() {

    }

    @Override
    public void onPackageSizeChanged(String packageName) {

    }

    @Override
    public void onAllSizesComputed() {

    }

    @Override
    public void onLauncherInfoChanged() {

    }

    @Override
    public void onLoadEntriesCompleted() {

    }
    
    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }
}
