package com.android.gphonemanager.clear;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Inflater;

import org.xmlpull.v1.XmlPullParser;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.R.integer;
import android.app.Activity;
import android.content.ClipData.Item;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.android.gphonemanager.adapter.ChannelInfoBean;
import com.android.gphonemanager.view.SmoothProgressView;
import com.android.gphonemanager.BaseActivity;
import com.android.gphonemanager.MainListView;
import com.android.gphonemanager.R;
import android.os.storage.VolumeInfo;
import android.text.format.Formatter;
import android.util.Log;

/*
 * 1. 缓存
 * 2. 安装包
 * 3. 系统垃圾
 * 4. 大文件
 * 5. 应用卸载
 * 6. SD卡文件
 * 7. 图片
 * 8. 视频
 * 
 * 
 * 1. 缓存与垃圾  （缓存， 安装包， 系统垃圾）
 * 2. 应用卸载 （应用列表）
 * 3. 大文件 （大文件）
 * 4. 图片 （图片）
 * 5. 音频和视频 （视频和音频）
 * 
 * (non-Javadoc)
 * @see com.android.gphonemanager.BaseActivity#onCreate(android.os.Bundle)
 */

public class ClearActivity extends BaseActivity {
	
	ArrayList<GroupItem> mGroupItems = new ArrayList<>();
	
	View mProgressView;
	TextView mStorageInfoText;
	TextView mStorageSDInfoText;
	TextView mScanInfoText;
	TextView mCountText;
	ListView mListView;
	ClearMainListAdapter mAdapter;
	SmoothProgressView smoothProgressView;
//	MainListAdapter mAdapter;
	private ArrayList<CleanListItem> mMainListItems = new ArrayList<>();
	
	CleanerService mCleanerService;
	Context mContext;
	
	private StorageManager mStorageManager;
	ScanActionListener mScanActionListener = new ScanActionListener();
	private boolean mAlreadyScanned = false;
	private ScanType scanType;
	
	private long mPrivateTotalBytes;
	private long mPublicTotalBytes;
	private long mPrivateUsedBytes;
	private long mPublicUsedBytes;
	
	private static final int DELETE_RESULT = 9;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.clear_activity);
		
		mContext = getApplicationContext();
		
		setTitle(getString(R.string.clear_key));
		
		mCountText = (TextView)findViewById(R.id.count_text);
		mStorageInfoText = (TextView)findViewById(R.id.info_text);
		mScanInfoText = (TextView)findViewById(R.id.scan_info_text);
		mListView = (ListView)findViewById(R.id.main_list);
		smoothProgressView = (SmoothProgressView)findViewById(R.id.progress_view);
//        mListView.setLayoutManager(new LinearLayoutManager(this));
//        mListView.setItemAnimator(new DefaultItemAnimator());
//        mListView.setHasFixedSize(true);
        
		mStorageSDInfoText = (TextView)findViewById(R.id.sdcard_info_text);
		
		scanType = new ScanType();
		
		initListData();
		mAdapter = new ClearMainListAdapter(mMainListItems);
//		mAdapter = new MainListAdapter(mContext, mMainListItems);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(mAdapter);
		
		mStorageManager = mContext.getSystemService(StorageManager.class);
		
		refresh();
		
		bindService(new Intent(mContext, CleanerService.class), 
				mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (requestCode) {
		case DELETE_RESULT:
			if (mCleanerService != null) {
				scanType = mCleanerService.getData();
				Log.i("jingyi", "onActivityResult");
				refresh();
				int item = data.getFlags();
				updateListItem(item);
				mAdapter.notifyDataSetChanged();
			}
			break;

		default:
			break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		unbindService(mServiceConnection);
		super.onDestroy();
	}
	
	private void refresh() {
		long totalSize = scanType.totalSize;
		mCountText.setText(Formatter.formatFileSize(mContext, totalSize));
		
		String scanInfo;
		
		if (mCleanerService != null) {
			if (!mCleanerService.onScaning()) {
				scanInfo = getString(R.string.junk_to_clean);
			} else {
				scanInfo = getString(R.string.scan_info_text, scanType.nowScaningPath);
			}
		} else {
			scanInfo = "";
		}
		mScanInfoText.setText(scanInfo);
		
		getStorageVolumes();
		smoothProgressView.setPercent((float)(scanType.usedSize)/(mPrivateTotalBytes+mPublicTotalBytes), 
				(float)scanType.totalSize/(mPrivateTotalBytes+mPublicTotalBytes));
	}

	
	public class ScanActionListener implements CleanerService.OnActionListener {

		@Override
		public void onScanStarted(Context context) {
			for (CleanListItem item : mMainListItems) {
				item.setIsRunning(true);
			}
			
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onScanProgressUpdated(Context context, List<String> apps) {
			
		}
		
		@Override
		public void onScanInfoUpdated(Context context) {
//			scanType = data;
			refresh();
		}

		@Override
		public void onScanCompleted(Context context, int type) {
			
			updateListItem(type);
			
			refresh();
			mAdapter.notifyDataSetChanged();
			
		}

		@Override
		public void onCleanStarted(Context context) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onCleanCompleter(Context context, long achaSize) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	private void updateListItem(int type) {
		switch (type) {
		
		case ScanType.ACTION_CACHE: {
			CleanListItem item = mMainListItems.get(type);
			item.setIsRunning(false);
			item.setSize(scanType.junkList.size+
					scanType.appInfoList.dataSize+
					scanType.appInfoList.cacheSize);
			break;
		}
		
		case ScanType.ACTION_APPUNINSTALL: {
			CleanListItem item = mMainListItems.get(type);
			item.setIsRunning(false);
			item.setSize(scanType.uninstallApp.size);
			break;
		}
		case ScanType.ACTION_BIGFILE: {
			CleanListItem item = mMainListItems.get(type);
			item.setIsRunning(false);
			item.setSize(scanType.bigFileList.size);
			break;
		}
		case ScanType.ACTION_MEDIA: {
			CleanListItem item = mMainListItems.get(type);
			item.setIsRunning(false);
			item.setSize(scanType.mediaList.size);
			break;
		}
		case ScanType.ACTION_PHOTO: {
			CleanListItem item = mMainListItems.get(type);
			item.setIsRunning(false);
			item.setSize(scanType.picList.size);
			break;
		}
			
		default:
			break;
		}
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i("jingyi", "onServiceDisconnected");
			mCleanerService = null;
			mCleanerService.setOnActionListener(null);
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i("jingyi", "onServiceConnected");
			mCleanerService = ((CleanerService.CleanerServiceBinder)service).getService();
			mCleanerService.setOnActionListener(mScanActionListener);
			
			if (!mCleanerService.onScaning() && !mAlreadyScanned) {

				
				mCleanerService.setData(scanType);
				mCleanerService.startAllScan();
			}
			//mCleanerService.star
			
		}
	};
	
	private void initListData() {
		TypedArray titleArray = getResources().obtainTypedArray(R.array.clean_titles);
		TypedArray imageArray = getResources().obtainTypedArray(R.array.clean_images);
		int len = titleArray.length();
		for (int i = 0; i < len; i++) {
			CleanListItem item = new CleanListItem(imageArray.getResourceId(i, 0),
					getString(titleArray.getResourceId(i, 0)));
			item.setAction(i);
			mMainListItems.add(item);
		}
		titleArray.recycle();
		imageArray.recycle();
	}
	
	private void onClickItem(int position) {
		
		Intent intent = new Intent(ClearActivity.this, MultiDeleteActivity.class);
//		Bundle bundle = new Bundle();
//		bundle.putSerializable("data", scanType.uninstallApp);
//		intent.putExtras(bundle);
		intent.setFlags(position);
		startActivityForResult(intent, DELETE_RESULT);
		/*
		switch (position) {
		case ScanType.ACTION_CACHE: {
			Intent intent = new Intent(ClearActivity.this, MultiDeleteActivity.class);
			intent.setFlags(position);
			startActivity(intent);
			break;
		}
		case ScanType.ACTION_APPUNINSTALL: {

			break;
		}
		case ScanType.ACTION_BIGFILE:
			
			break;

		case ScanType.ACTION_PHOTO:
			
			break;

		case ScanType.ACTION_MEDIA:
			
			break;

		default:
			break;
		}*/
		
	}
	
//	public class MainListAdapter extends RecyclerView.Adapter<MainListAdapter.ViewHolder> {
//
//		List<CleanListItem> mListData;
//		Context context;
//		private LayoutInflater mInflater;
//		
//	    public MainListAdapter(Context context, List<CleanListItem> data) {
//	        this.context = context;
//	        this.mListData = data;
//	        this.mInflater = LayoutInflater.from(context);
//	    }
//	    
//
//	    @Override
//	    public int getItemCount() {
//	        return mListData == null ? 0 : mListData.size();
//	    }
//	    
//	    @Override
//	    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//	        View view = mInflater.inflate(R.layout.clean_main_list_item, parent, false);
//	        
//	        ViewHolder holder = new ViewHolder(view);
//	        
//	        return holder;
//	    }
//	    
//	    @Override
//	    public void onBindViewHolder(ViewHolder holder, int position) {
//	        final CleanListItem item = mListData.get(position);
//	        
//			if (item != null) {
//				holder.icon.setImageResource(item.getIconRes());
//				holder.title.setText(item.getTitle());
//
//				if (item.isRunning) {
//					holder.runningText.setVisibility(View.VISIBLE);
//					holder.size.setVisibility(View.INVISIBLE);
//					holder.eEnterIcon.setVisibility(View.INVISIBLE);
//					//convertView.setEnabled(false);
//				} else {
//					holder.runningText.setVisibility(View.INVISIBLE);
//					holder.size.setVisibility(View.VISIBLE);
//					holder.eEnterIcon.setVisibility(View.VISIBLE);
//					holder.size.setText(item.getSizeStr());
//					//convertView.setEnabled(true);
//				}
//			}
//	    }
//	    
//	    class ViewHolder extends RecyclerView.ViewHolder{
//	        ImageView icon;
//	        TextView title;
//	        TextView size;
//	        ImageView eEnterIcon;
//	        TextView runningText;
//
//	        String key;
//	        
//	        public ViewHolder(View view) {
//	        	super(view);
//				icon = (ImageView)view.findViewById(R.id.icon);
//				title = (TextView)view.findViewById(R.id.title);
//				size = (TextView)view.findViewById(R.id.size);
//				eEnterIcon = (ImageView)view.findViewById(R.id.enter_icon);
//				runningText = (TextView)view.findViewById(R.id.scaning_text);
//	        }
//	    }
//		
//	}
	
	
	
	
	private class ClearMainListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
		
		ArrayList<CleanListItem> mListData;
		
		public ClearMainListAdapter(ArrayList<CleanListItem> data) {
			mListData = data;
		}
		
		public void swapData(ArrayList<CleanListItem> data) {
			mListData = data;

		}
		
		@Override
		public int getCount() {
			return mListData.size();
		}

		@Override
		public Object getItem(int position) {
			return mListData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder holder = null;
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.clean_main_list_item, parent, false);
				holder = new ViewHolder();
				holder.icon = (ImageView)convertView.findViewById(R.id.icon);
				holder.title = (TextView)convertView.findViewById(R.id.title);
				holder.size = (TextView)convertView.findViewById(R.id.size);
				holder.eEnterIcon = (ImageView)convertView.findViewById(R.id.enter_icon);
				holder.runningText = (TextView)convertView.findViewById(R.id.scaning_text);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			CleanListItem item = mListData.get(position);
			if (item != null) {
				holder.icon.setImageResource(item.getIconRes());
				holder.title.setText(item.getTitle());

				if (item.isRunning) {
					holder.runningText.setVisibility(View.VISIBLE);
					holder.size.setVisibility(View.INVISIBLE);
					holder.eEnterIcon.setVisibility(View.INVISIBLE);
					//convertView.setEnabled(false);
				} else {
					holder.runningText.setVisibility(View.INVISIBLE);
					holder.size.setVisibility(View.VISIBLE);
					holder.eEnterIcon.setVisibility(View.VISIBLE);
					holder.size.setText(item.getSizeStr());
					//convertView.setEnabled(true);
				}
			}
			
			if (position == (mListData.size()-1)) {
				View view = (View)convertView.findViewById(R.id.divider_line);
				view.setVisibility(View.GONE);
			}
			
			return convertView;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			onClickItem(position);
			
		}
		
		@Override
		public boolean isEnabled(int position) {
			// TODO Auto-generated method stub
			CleanListItem item = mListData.get(position);
			return !item.isRunning && item.getSize() != 0;
		}
		
	    class ViewHolder {
	        ImageView icon;
	        TextView title;
	        TextView size;
	        ImageView eEnterIcon;
	        TextView runningText;

	        String key;
	    }
	}
	
	public class CleanListItem {
		private int action;
		private boolean isRunning = false;
		private long size = 0;
		private String sizeStr = "0KB";
		
		public CleanListItem(int icon, String title) {
			iconRes =  icon;
			this.title = title;
		}
		
		String title;
		int iconRes;
		
		public long getSize() {
			return size;
		}
		
		public void setSize(long size) {
			this.size = size;
			sizeStr = Formatter.formatFileSize(mContext, size);
		}
		
		public String getSizeStr() {
			return sizeStr;
		}
		
		public int getIconRes() {
			return iconRes;
		}
		public String getTitle() {
			return title;
		}
		
		public void setAction(int action) {
			this.action = action;
		}
		
		public void setIsRunning(boolean bool) {
			this.isRunning = bool;
		}
		
		public int getAction() {
			return action;
		}
		
		public boolean getRunning() {
			return isRunning;
		}
	}

	private void getStorageVolumes() {
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        
        mPublicTotalBytes = 0;
        mPrivateTotalBytes = 0;
     
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                if (vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    mPrivateUsedBytes = path.getTotalSpace() - path.getFreeSpace();
                    mPrivateTotalBytes = path.getTotalSpace();
                    
                    String info = getString(R.string.phone_storage_info, 
                    		Formatter.formatFileSize(mContext, mPrivateUsedBytes), 
                    		Formatter.formatFileSize(mContext, mPrivateTotalBytes));
                    
                    mStorageInfoText.setText(info);
                }
                
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                if (vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    
                    mPublicUsedBytes = path.getTotalSpace() - path.getFreeSpace();
                    mPublicTotalBytes = path.getTotalSpace();

                    String info = getString(R.string.sd_card_info, 
                    		Formatter.formatFileSize(mContext, mPublicUsedBytes), 
                    		Formatter.formatFileSize(mContext, mPublicTotalBytes));
                    
                    mStorageSDInfoText.setText(info);
                }
            }
        }
        
        
	}
}
