package com.android.gphonemanager;

import java.util.ArrayList;
import java.util.List;

import com.android.gphonemanager.adapter.ChannelInfoBean;
import com.android.gphonemanager.applock.FPRecognizeActivity;
import com.android.gphonemanager.clear.Scaning;
import com.android.gphonemanager.freezeapp.FreezeActivity;
import com.android.gphonemanager.view.CircleProgressView;
import com.android.gphonemanager.view.GridViewGallery;
import com.android.gphonemanager.netmanager.NetControlerActivity2;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import com.android.gphonemanager.clear.ClearActivity;
import android.content.ComponentName;

public class MainActivity extends Activity {
	private static final String TAG = "Gmanager.MainActivity";

	
	private static final int ID_FREEZE_APP = 0;
	private static final int ID_APP_LOCK = 1;
	private static final int ID_CLEAR = 2;
	private static final int ID_PERMISSION_MANAGER = -1;
	private static final int ID_NET_MANAGER = 3;
	private static final int ID_APP_MANAGER = 4;

	//private PercentView mRamPercentView;
	private CircleProgressView mCircleProgressView;
	private TextView mClearKey;
	private TextView mRamPercentData;
	private GridViewGallery mGridViewGallery;
	private LinearLayout mGridGalleryContainer;

	private ArrayList<ChannelInfoBean> mMainListItems = new ArrayList<>();
	//private MainListAdatper mainListAdatper;
	private long mRamTotal;
	private long mRamAvailable;
	private float percent;
	private String percentData;

	private ActivityManager mActivityManager;
	Context mContext;
	
	private Handler H = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			default:
				break;
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main_activity);

		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		mContext = getApplicationContext();
		
		initViews();
		
		MemoryInfo mi = new MemoryInfo();
		mActivityManager.getMemoryInfo(mi);
		refreshMemoryUI(mi);
		
		initMainListItems();
		mGridViewGallery = new GridViewGallery(MainActivity.this, mMainListItems);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
		mGridGalleryContainer.addView(mGridViewGallery, params);
		
		//getActionBar().hide();
		
		
		/*mainListAdatper = new MainListAdatper(this, mMainListItems);
		mListView.setAdapter(mainListAdatper);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				switch ((int) id) {
				case ID_APP_LOCK: {
					Intent intent = new Intent(MainActivity.this,
							FPRecognizeActivity.class);
					startActivity(intent);
					break;
				}
				case ID_FREEZE_APP: {
					Intent intent = new Intent(MainActivity.this,
							FreezeActivity.class);
					startActivity(intent);
					break;
				}
				case ID_CLEAR: {
					Intent intent = new Intent(MainActivity.this, Scaning.class);
					startActivity(intent);
					break;
				}
				case ID_NET_MANAGER: {

					break;
				}

				default:
					break;
				}
			}
		});*/
	}
	
	private void initViews() {
		mGridGalleryContainer = (LinearLayout) findViewById(R.id.grid_gallery_container);
		//mRamPercentView = (PercentView) findViewById(R.id.ram_percent_view);
		mCircleProgressView = (CircleProgressView) findViewById(R.id.ram_percent_view);
		mRamPercentData = (TextView) findViewById(R.id.ram_percent_data);
		
		mClearKey = (TextView) findViewById(R.id.clear_key);
		mClearKey.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				
				
				new CleanTask().execute();
			}
		});
	}
	
	private void refreshMemoryUI(MemoryInfo mInfo) {
		mRamTotal = mInfo.totalMem;
		mRamAvailable = mInfo.availMem;
		mRamTotal = updateTotalRamSize(mRamTotal);
		percent = (mRamTotal - mRamAvailable) * 100.0f / mRamTotal;
		//mRamPercentView.setPercent(percent);
		mCircleProgressView.setPercent((int)percent);
        
		percentData = getPercentData(mRamTotal - mRamAvailable, mRamTotal);
		mRamPercentData.setText(percentData);
	}
	
	private class CleanTask extends AsyncTask<Void, Object, MemoryInfo> {

		private long beforeMemory;
		
		@Override
		protected void onPreExecute() {
			mCircleProgressView.setStartAnim();
			mClearKey.setEnabled(false);
		}
		
		@Override
		protected MemoryInfo doInBackground(Void... params) {
			
	    	if (mActivityManager == null) {
	    		mActivityManager = (ActivityManager)  
		                getSystemService(Context.ACTIVITY_SERVICE);  
	    	}
	        List<RunningAppProcessInfo> appProcessInfos = mActivityManager  
	                .getRunningAppProcesses();  
	        
			MemoryInfo mi = new MemoryInfo();
			mActivityManager.getMemoryInfo(mi);
			beforeMemory = mi.availMem;
	          
	        for (RunningAppProcessInfo appProcessInfo:appProcessInfos) {  
	            if( appProcessInfo.processName.contains("com.android.system")  
	                    ||appProcessInfo.pid==android.os.Process.myPid())
	                continue;  
	            if (appProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_SERVICE) {
		            String[] pkNameList=appProcessInfo.pkgList;
		            for(int i=0;i<pkNameList.length;i++){  
		                String pkName=pkNameList[i];  
			           
		                String label = "";
			        try {
			        	PackageManager packageMan = getPackageManager();
						ApplicationInfo applicationInfo = packageMan.getApplicationInfo(pkName, 0);
						label = (String)packageMan.getApplicationLabel(applicationInfo);
						
					} catch (NameNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		                
		                mActivityManager.getMemoryInfo(mi);
		                publishProgress(mi, label); //appProcessInfo.processName
		                
		                mActivityManager.killBackgroundProcesses(pkName);
		            }  
	            }
	        }
	        
            mActivityManager.getMemoryInfo(mi);
            
			return mi;
		}
		
		@Override
		protected void onPostExecute(MemoryInfo result) {
			refreshMemoryUI(result);
			
			long cleanSize = result.availMem-beforeMemory;
			cleanSize = cleanSize < 0 ? 0 : cleanSize;
			
			String cleanUpString = Formatter.formatFileSize(mContext, cleanSize);
			String info = getResources().getString(R.string.clean_up_ram_size, cleanUpString);
			mCircleProgressView.setInfo(info);
			mCircleProgressView.stop();
			mClearKey.setEnabled(true);
		}
		
		@Override
		protected void onProgressUpdate(Object... values) {
			MemoryInfo memoryInfo = (MemoryInfo)values[0];
			String info = (String)values[1];
			
			refreshMemoryUI(memoryInfo);
			mCircleProgressView.setInfo(info);
		}
		
	}
	
	private void initMainListItems() {
		TypedArray titleArray = getResources().obtainTypedArray(R.array.actions_titles);
		TypedArray imageArray = getResources().obtainTypedArray(R.array.actions_images);
		int len = titleArray.length();
		for (int i = 0; i < len; i++) {
			ChannelInfoBean item = new ChannelInfoBean(i, imageArray.getResourceId(i, 0), 
					getString(titleArray.getResourceId(i, 0)));
			
			mMainListItems.add(item);
			
			item.setOnClickListener(new ChannelInfoBean.onGridViewItemClickListener(){

				@Override
				public void ongvItemClickListener(int position) {
					switch (position) {
						case ID_FREEZE_APP: {
							Intent intent = new Intent(MainActivity.this,
									FreezeActivity.class);
							startActivity(intent);
							break;
						}
						case ID_APP_LOCK: {
							Intent intent = new Intent(MainActivity.this,
									FPRecognizeActivity.class);
							startActivity(intent);
							break;
						}
						case ID_CLEAR: {
							//Intent intent = new Intent(MainActivity.this, Scaning.class);
							Intent intent = new Intent(MainActivity.this, ClearActivity.class);
							startActivity(intent);
							break;
						}
						case ID_PERMISSION_MANAGER:{
							break;
						}
						case ID_NET_MANAGER:{
//                            Intent intent = new Intent().setComponent(new ComponentName ("com.android.settings","com.android.settings.Settings$DataUsageSummaryActivity"));
//                            startActivity(intent);
                            
							Intent intent = new Intent(MainActivity.this,
									NetControlerActivity2.class);
							startActivity(intent);
                            break;
                        }
						case ID_APP_MANAGER: {
				            try {
				                Intent intent =new Intent();
				                intent.setClassName("com.android.autorunmanager", "com.android.autorunmanager.AutorunMain");
				                startActivity(intent);//A by shubin for RGKautoStartManager
				            } catch (Exception e) {
				                //do nothing
				            }
							break;
						}
						default:
							break;
					}
				}});
		}
		
		titleArray.recycle();
		imageArray.recycle();
	}
	
	
//	private void initMainListItems() {
//		MainListItem item0 = new MainListItem(ID_APP_LOCK,
//				R.drawable.ic_app_lock, R.string.app_lock,
//				R.string.app_lock_summary);
//		MainListItem item1 = new MainListItem(ID_FREEZE_APP,
//				R.drawable.ic_freeze_app, R.string.freeze_app,
//				R.string.freeze_app_summary);
//		MainListItem item2 = new MainListItem(ID_CLEAR, R.drawable.ic_ram,
//				R.string.clear, R.string.clear_summary);
//		MainListItem item3 = new MainListItem(ID_NET_MANAGER,
//				R.drawable.ic_net_manager, R.string.net_manager,
//				R.string.net_manager_summary);
//		mMainListItems.add(item0);
//		mMainListItems.add(item1);
//		mMainListItems.add(item2);
//		mMainListItems.add(item3);
//
//		/*
//		 * MainListItem item4 = new MainListItem(5, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary); MainListItem
//		 * item5 = new MainListItem(6, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary); MainListItem
//		 * item6 = new MainListItem(7, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary); MainListItem
//		 * item7 = new MainListItem(8, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary); MainListItem
//		 * item8 = new MainListItem(9, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary); MainListItem
//		 * item9 = new MainListItem(10, R.drawable.ic_launcher,
//		 * R.string.net_manager, R.string.net_manager_summary);
//		 * mMainListItems.add(item4); mMainListItems.add(item5);
//		 * mMainListItems.add(item6); mMainListItems.add(item7);
//		 * mMainListItems.add(item8); mMainListItems.add(item9);
//		 */
//	}

	/*class MainListAdatper extends BaseAdapter {

		ArrayList<MainListItem> items;
		Context mContext;
		LayoutInflater mInflater;

		public MainListAdatper(Context context, ArrayList<MainListItem> items) {
			mContext = context;
			mInflater = LayoutInflater.from(mContext);
			this.items = items;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		@Override
		public Object getItem(int position) {
			return items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return items.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			if (convertView == null || convertView.getTag() == null) {
				viewHolder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.main_list_item, null);

				viewHolder.icon = (ImageView) convertView
						.findViewById(R.id.icon);
				viewHolder.title = (TextView) convertView
						.findViewById(R.id.title);
				viewHolder.summary = (TextView) convertView
						.findViewById(R.id.summary);

				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}

			MainListItem item = items.get(position);
			viewHolder.icon.setImageResource(item.iconRes);
			viewHolder.title.setText(item.titleRes);
			viewHolder.summary.setText(item.summaryRes);

			return convertView;
		}

		class ViewHolder {
			ImageView icon;
			TextView title;
			TextView summary;
		}
	}

	class MainListItem {
		public MainListItem(int id, int iconRes, int titleRes, int summaryRes) {
			this.id = id;
			this.iconRes = iconRes;
			this.titleRes = titleRes;
			this.summaryRes = summaryRes;
		}

		int id;
		int iconRes;
		int titleRes;
		int summaryRes;
	}*/
	
	private String getPercentData(long used, long total) {
		String usedSize = Formatter.formatFileSize(this, used);
		String totalSize = Formatter.formatFileSize(this, total);
		
		String format = getResources().getString(R.string.ram_size, usedSize + "/" + totalSize);
		
		return format;
	}
	
	private long updateTotalRamSize(long mtotalSize) {
		if(mtotalSize < 512*1024*1024L){
			mtotalSize = 512*1024*1024L;
		}else{
			int data = (int)(mtotalSize  / 1024 / 1024 / 1024);
			mtotalSize = (long)(data+1)*1024*1024*1024L ;
		}
		return mtotalSize;
	}
}
