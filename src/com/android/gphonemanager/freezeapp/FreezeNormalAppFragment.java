package com.android.gphonemanager.freezeapp;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gphonemanager.R;

public class FreezeNormalAppFragment extends Fragment implements OnItemClickListener {
	private static final String TAG = "Gmanager.FPFreezeNormalAppFragment";
	private Activity mActivity;

	private OnItemClickListener mOnItemClickListener;

	private GridView mGridView;
	private AppsAdapter mAppsAdapter;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.freeze_normal_house_fragment, null);
		mGridView = (GridView) rootView.findViewById(R.id.myGrid);
		mGridView.setVisibility(View.VISIBLE);
		mGridView.setOnItemClickListener(this);
		mAppsAdapter = new AppsAdapter(mActivity);
		mGridView.setAdapter(mAppsAdapter);

		return rootView;
	}

	public GridView getGridView() {
		return mGridView;
	}


	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}

	interface OnItemClickListener {
		void onItemClick(AdapterView<?> parent, View itemView, FreezeAppInfo info);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View itemView, int position,
			long id) {
		if (mOnItemClickListener != null) {
			mOnItemClickListener.onItemClick(parent, itemView,
					(FreezeAppInfo) mAppsAdapter.getItem(position));
		}

	}
	
	public void changeData(List<FreezeAppInfo> apps) {
		Log.d(TAG, "changeData");
		mAppsAdapter.changeData(apps);
	}
	
	public void addItem(FreezeAppInfo app) {
		mAppsAdapter.addItem(app);
	}
	
	public void deleteItem(FreezeAppInfo app) {
		mAppsAdapter.deleteItem(app);
	}

	public class AppsAdapter extends BaseAdapter {
		private Context mContext;
		private LayoutInflater mInflater;
		private List<FreezeAppInfo> mApps;

		public AppsAdapter(Context context) {
			mContext = context;
			mInflater = LayoutInflater.from(mContext);
		}

		public void changeData(List<FreezeAppInfo> apps) {
			if (apps != null && mApps == null) {
				mApps = apps;
			}
			notifyDataSetChanged();
		}
		
		public void addItem(FreezeAppInfo app) {
			app.isFreezed = false;
			mApps.add(app);
			notifyDataSetChanged();
		}
		
		public void deleteItem(FreezeAppInfo app) {
			mApps.remove(app);
			notifyDataSetChanged();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.freeze_normal_house_item,
						null);
				viewHolder.icon = (ImageView) convertView
						.findViewById(R.id.app_icon);
				viewHolder.text = (TextView) convertView
						.findViewById(R.id.app_name);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			FreezeAppInfo app = mApps.get(position);
			viewHolder.icon.setImageDrawable(app.icon);
			viewHolder.text.setText(app.name);

			return convertView;
		}

		public final int getCount() {
			return mApps == null ? 0 : mApps.size();
		}

		public final Object getItem(int position) {
			return mApps.get(position);
		}

		public final long getItemId(int position) {
			return position;
		}
	}

	class ViewHolder {
		ImageView icon;
		TextView text;
	}
}
