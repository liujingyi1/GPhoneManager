package com.android.gphonemanager.netmanager;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import com.android.gphonemanager.R;
import com.android.gphonemanager.netmanager.SubInfoUtils;
import com.android.gphonemanager.netmanager.Constant;

public class MobileFragment extends NetControlBaseFragment {
	AppItemAdapter mAppItemAdapter = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getContext();

		View view = inflater.inflate(R.layout.net_control_fragment, null);
		mAppListView = (ListView) view.findViewById(R.id.listview_app);
		mProgressBar = view.findViewById(R.id.progressBar);
		
		mAppItemAdapter = new AppItemAdapter();
		mAppListView.setAdapter(mAppItemAdapter);
		
		Log.i("jingyi", "onCreateView");
		
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
				viewHolder.sim1Datausage = (TextView) convertView.findViewById(R.id.sim1_data);
				viewHolder.sim2Datausage = (TextView) convertView.findViewById(R.id.sim2_data);
				viewHolder.enable = (Switch) convertView.findViewById(R.id.net_enable);
				viewHolder.enable.setOnClickListener(mOnClickListener);
				viewHolder.mobileContainer.setVisibility(View.VISIBLE);
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
			
			viewHolder.enable.setTag(appUsage);
			viewHolder.icon.setImageDrawable(appUsage.icon);
			viewHolder.appName.setText(appUsage.name);
			if (sim1Enable) {
				viewHolder.sim1Datausage.setText(Formatter.formatFileSize(mContext, appUsage.sim1Total));
			}
			if (sim2Enable) {
				viewHolder.sim2Datausage.setText(Formatter.formatFileSize(mContext, appUsage.sim2Total));
			}
			
			boolean enabled = SharePreferenceUtils.getEnabled(getActivity());
			boolean check = (appUsage.selected_mobile || enabled);
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
					Switch enable = (Switch)view;
					if (appDetail.selected_mobile) {
						enable.setChecked(!appDetail.selected_mobile);
						mFirewallDatabaseHelper.insertDenyUidWithMobile(appDetail.uid);
					} else {
						enable.setChecked(!appDetail.selected_mobile);
						mFirewallDatabaseHelper.deleteDenyUidWithMobile(appDetail.uid);
					}
					
					Toast.makeText(getActivity(), R.string.set_net_able_success, 1000).show();
					break;
			}
		}
	};
}
