package com.android.gphonemanager.netmanager;

import java.util.ArrayList;

import com.android.gphonemanager.netmanager.FirewallDatabaseHelper;
import com.android.gphonemanager.netmanager.NetAdapter;
import com.android.gphonemanager.netmanager.MobileFragment.AppItemAdapter;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

public class NetControlBaseFragment extends Fragment {
	Context mContext;
	
	ListView mAppListView;
	
	View mProgressBar;
	
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
			
			showProcessBar(false);
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<AppUsage>> arg0) {
			// TODO Auto-generated method stub
			
		}
    };
    
    public void changeData(ArrayList<AppUsage> data) {
    	
    }
    
	public void refleshUI() {

		
	}
	
	public void reLoad() {
		
        Bundle extras = new Bundle();
        extras.putBoolean("only_user_app", mOnlyUserApp);
		
		getLoaderManager().restartLoader(Constant.LOADER_ID_FOR_FIREWALL, extras, mAppsLoaderCallbacks);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		reLoad();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFirewallDatabaseHelper = new FirewallDatabaseHelper(getActivity());
		mFirewallService = NetAdapter.getInstance(this.getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}
	
	public void showProcessBar(boolean show) {
		if (mProgressBar != null) {
			if (show) {
				mProgressBar.setVisibility(View.VISIBLE);
			} else {
				mProgressBar.setVisibility(View.GONE);
			}
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
		Switch enable;
	}
}
