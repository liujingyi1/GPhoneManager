package com.android.gphonemanager.freezeapp;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gphonemanager.R;

public class FreezeAppFragment extends Fragment implements
		OnItemClickListener{
	private static final String TAG = "Gmanager.FPFreezeAppFragment";
	
	private final static int EXTRA_ITEM_COUNT = 1;
	private final static int ITEM_TYPE_COMMON = 0;
	private final static int ITEM_TYPE_EXTRA = 1;

	private GridView mGridView;
	private AppsAdapter mAppsAdapter;
	
	private Activity mActivity;
	
	private OnItemClickListener mOnItemClickListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.freeze_house_fragment, null);
		mGridView = (GridView) rootView.findViewById(R.id.myGrid);
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
		void onItemClick(AdapterView<?> parent, View itemView, int position,
				FreezeAppInfo info);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View itemView, int position,
			long id) {
		if (mOnItemClickListener != null) {
			if (position <= EXTRA_ITEM_COUNT - 1) {
				mOnItemClickListener.onItemClick(parent, itemView, position,
						null);
			} else {
				mOnItemClickListener.onItemClick(parent, itemView, position,
						(FreezeAppInfo) mAppsAdapter.getItem(position));
			}
		}

	}

	private LayerDrawable getStaticDrawable(Drawable appIcon) {
		Drawable[] drawables = new Drawable[2];
		drawables[0] = appIcon;
		drawables[1] = getResources().getDrawable(R.drawable.icon_ice);
		LayerDrawable ld = new LayerDrawable(drawables);
		return ld;
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
			if (apps != null) {
				mApps = apps;	
			}
			Log.d(TAG, "changeData");
			notifyDataSetChanged();
		}
		
		public void addItem(FreezeAppInfo app) {
			app.isFreezed = true;
			mApps.add(app);
			notifyDataSetChanged();
		}
		
		public void deleteItem(FreezeAppInfo app) {
			mApps.remove(app);
			notifyDataSetChanged();
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (position == 0) {
				View v = mInflater.inflate(R.layout.freeze_add_app_item, null);
				return v;
			}
			if (convertView == null) {
				viewHolder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.freeze_house_item,
						null);
				viewHolder.icon = (ImageView) convertView
						.findViewById(R.id.app_icon);
				viewHolder.text = (TextView) convertView
						.findViewById(R.id.app_name);
				viewHolder.over = (ImageView) convertView
						.findViewById(R.id.ice_over);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			FreezeAppInfo app = mApps.get(position - EXTRA_ITEM_COUNT);
			viewHolder.icon.setImageDrawable(app.icon);
			viewHolder.text.setText(app.name);

			return convertView;
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0) {
				return ITEM_TYPE_EXTRA;
			} else {
				return ITEM_TYPE_COMMON;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		public final int getCount() {
			return (mApps == null || mApps.size() == 0 ? 0 : mApps.size())
					+ EXTRA_ITEM_COUNT;
		}

		public final Object getItem(int position) {
			return mApps.get(position - EXTRA_ITEM_COUNT);
		}

		public final long getItemId(int position) {
			return position;
		}
	}

	class ViewHolder {
		ImageView icon;
		TextView text;
		ImageView over;
	}
}
