package com.android.gphonemanager.netmanager;

import java.util.ArrayList;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gphonemanager.R;
import com.android.gphonemanager.netmanager.SubInfoUtils;
import com.android.gphonemanager.netmanager.Constant;

public class WifiFragment extends NetControlBaseFragment {
	private AppItemAdapter mAppItemAdapter = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		mOnlyUserApp = true;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getContext();

		View view = inflater.inflate(R.layout.net_control_fragment, null);
		mAppListView = (ListView) view.findViewById(R.id.listview_app);
		mProgressBar = view.findViewById(R.id.progressBar);
		
		mAppItemAdapter = new AppItemAdapter();
		mAppListView.setAdapter(mAppItemAdapter);
		
		return view;
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
    public void changeData(ArrayList<AppUsage> data) {
    	mAppItemAdapter.changeData(data);
    }
    
    @Override
    public void refleshUI() {
		if (mAppItemAdapter != null) {
			mAppItemAdapter.notifyDataSetInvalidated();
		}
    }
	
	class AppItemAdapter extends BaseAdapter {
		
		private ArrayList<AppUsage> appUsages = null;
		
		public AppItemAdapter() {
			appUsages = new ArrayList<AppUsage>();
			
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
				convertView = inflater.inflate(R.layout.net_control_item_view, null);
				viewHolder = new ViewHolder();
				viewHolder.icon = (ImageView)convertView.findViewById(R.id.image);
				viewHolder.appName = (TextView)convertView.findViewById(R.id.label);
				viewHolder.mobileContainer = convertView.findViewById(R.id.mobile_usage);
				viewHolder.wifiContainer = convertView.findViewById(R.id.wifi_usage);
				viewHolder.wifiDatausage = (TextView) convertView.findViewById(R.id.wifi_data);
				viewHolder.enable = (Switch) convertView.findViewById(R.id.net_enable);
				viewHolder.enable.setOnClickListener(mOnClickListener);
				viewHolder.wifiContainer.setVisibility(View.VISIBLE);
				
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder)convertView.getTag();
			}
			
			AppUsage appUsage = appUsages.get(position);
			
			viewHolder.enable.setTag(appUsage);
			viewHolder.icon.setImageDrawable(appUsage.icon);
			viewHolder.appName.setText(appUsage.name);
			viewHolder.wifiDatausage.setText(Formatter.formatFileSize(mContext, appUsage.wifiTotal));
			
			boolean enabled = SharePreferenceUtils.getEnabled(getActivity());
			boolean check = (appUsage.selected_wifi || enabled);
			viewHolder.enable.setChecked(!check);

			if (enabled) {
				viewHolder.enable.setEnabled(false);
			} else {
				viewHolder.enable.setEnabled(true);
			}
			
			return convertView;
		}
		
		public void changeData(ArrayList<AppUsage> appDetails){
			this.appUsages = appDetails;
			this.notifyDataSetChanged();
		}
	}
	
	private OnClickListener mOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {
			AppUsage appDetail = (AppUsage)view.getTag();
			switch (view.getId()) {
				case R.id.net_enable:
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
					Switch enable = (Switch)view;
					if (appDetail.selected_wifi) {
						enable.setChecked(!appDetail.selected_wifi);
						mFirewallDatabaseHelper.insertDenyUidWithWifi(appDetail.uid);
					} else {
						enable.setChecked(!appDetail.selected_wifi);
						mFirewallDatabaseHelper.deleteDenyUidWithWifi(appDetail.uid);
					}
					
					Toast.makeText(getActivity(), R.string.set_net_able_success, 1000).show();
					break;
					
			}
		}
	};
}
