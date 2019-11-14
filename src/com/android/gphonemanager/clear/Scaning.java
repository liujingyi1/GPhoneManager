package com.android.gphonemanager.clear;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.gphonemanager.R;
import com.android.gphonemanager.clear.BigFileAdapter.ViewHolder;
import com.android.gphonemanager.clear.MyListView.OnRefreshLoadingMoreListener;

import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings.NameValueTable;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;

public class Scaning extends Activity implements OnClickListener,
		OnRefreshLoadingMoreListener {
	ArrayList<String> remove_s = new ArrayList<String>();
	private ArrayList<File> fileList = new ArrayList<File>();
	private ArrayList<File> fileSDList = new ArrayList<File>();
	private ArrayList<File> file_adap;
	private MyListView listadapter;
	private Button but_all, popup, but_delect, but_no, but_yes;
	private BigFileAdapter adapter;
	private TextView text_total;
	private ImageView iv_open;
	private double total;
	private int select_size = 0;
	private int position;
	ArrayList<File> remove_file = new ArrayList<File>();
	RelativeLayout RelativeLayout_delect, RelativeLayout_sure, popu_rela,
			jiazai_rela;
	int choose = 0;
	boolean b = false;
	boolean isOpen = true;
	int num = 0;
	private SharedPreferences preferences;
	PackageManager mPackageManager;

	//add liujingyi start
    private HandlerThread mThread;
    private BackgroundHandler mHandler;
	List<ResolveInfo> mAppEntrys = new ArrayList<ResolveInfo>();
    private HashMap<String, APPInfo> mEntriesMap = new HashMap<String, APPInfo>();
    private ArrayList<File> apkFileList = new ArrayList<File>();
    
    private static final int MAX_DEPTH = 0xff;
    
    private static final int APK_ERROR = -1;
    private static final int INSTALLED = 0;
    private static final int UNINSTALLED = 1;
    private static final int INSTALLED_UPDATE = 2;
    
    private static String[] mLogPath = {"/data/local/tmp/",
    									"/data/tmp/",
    									"/data/system/usagestats/",
    									"/data/system/appusagestats/",
    									"/data/system/dropbox/",
    									"/data/tombstones/",
    									"/data/anr/",
    									"/data/log/main/"};
    private ArrayList<File> logFileList = new ArrayList<File>();
    private ArrayList<File> sytemLogFileList = new ArrayList<File>();
	//add liujingyi end
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.scan_file);
		findViewId();
		preferences = getSharedPreferences("config", Context.MODE_PRIVATE);
		select_size = preferences.getInt("fileSize", 10);
		Log.i("159", "preferences" + select_size);
		but_all.setText(choose + "");
	
		mPackageManager = getPackageManager();
		
		new Thread(thread_scan).start();
		
		//add liujingyi start
        mThread = new HandlerThread("Scaning.worker", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mHandler = new BackgroundHandler(mThread.getLooper());
        init();
        //add liujingyi end
        
		listadapter.setItemsCanFocus(false);
		listadapter.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listadapter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				ViewHolder vHolder = (ViewHolder) arg1.getTag();
				vHolder.checkbox.toggle();
				BigFileAdapter.isSelected.put(arg2 - 1,
						vHolder.checkbox.isChecked());
				totalFile(arg2 - 1);
				text_total.setVisibility(View.VISIBLE);
				text_total.setText(MyUtil.FileSize(total));
				Set keyset = BigFileAdapter.isSelected.keySet();
				Iterator it = keyset.iterator();
				while (it.hasNext()) {
					int k = (Integer) it.next();
					Boolean v = BigFileAdapter.isSelected.get(k);
					if (v.equals(true)) {
						but_delect.setEnabled(true);

						break;
					} else {
						but_delect.setEnabled(false);

					}
				}
			}
		});
		listadapter.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				String name = file_adap.get(arg2 - 1).getName();
				String path = file_adap.get(arg2 - 1).getAbsolutePath();
				String size = MyUtil.FileSize((double) file_adap.get(arg2 - 1)
						.length());
				// dialog.show(getFragmentManager(), "MyDialogFragment");
				Intent intent = new Intent(Scaning.this, MyActivityDialog.class);
				intent.putExtra("name", name);
				intent.putExtra("path", path);
				intent.putExtra("size", size);
				startActivity(intent);
				return true;
			}
		});

		popup.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (isOpen) {

					iv_open.setImageResource(R.drawable.clear_show_down);
					isOpen = false;
				} else {
					iv_open.setImageResource(R.drawable.clear_show_up);
					isOpen = true;
				}
				showPopupWindow(view);

			}
		});

	}

	Runnable thread_spin = new Runnable() {
		public void run() {
			file_adap = new ArrayList<File>();
			if (position == 0) {
				file_adap = fileList;
			} else if (position == 1) {

				for (int i = 0; i < fileList.size(); i++) {
					if (fileList.get(i).getName().endsWith(".zip")
							|| fileList.get(i).getName().endsWith(".rar")) {

						file_adap.add(fileList.get(i));
					}
				}

			} else if (position == 2) {

				for (int i = 0; i < fileList.size(); i++) {
					if (fileList.get(i).getName().endsWith(".mp4")
							|| fileList.get(i).getName().endsWith(".avi")
							|| fileList.get(i).getName().endsWith(".rmvb")) {

						file_adap.add(fileList.get(i));
					}
				}
			} else if (position == 3) {

				for (int i = 0; i < fileList.size(); i++) {
					if (fileList.get(i).getName().endsWith(".mp3")
							|| fileList.get(i).getName().endsWith(".wma")) {
						file_adap.add(fileList.get(i));
					}
				}
			} else if (position == 4) {

				for (int i = 0; i < fileList.size(); i++) {
					if (fileList.get(i).getName().endsWith(".txt")
							|| fileList.get(i).getName().endsWith(".doc")) {
						file_adap.add(fileList.get(i));
					}
				}

			} else if (position == 5) {
				for (int i = 0; i < fileList.size(); i++) {
					if (!fileList.get(i).getName().endsWith(".zip")
							&& !fileList.get(i).getName().endsWith(".rar")
							&& !fileList.get(i).getName().endsWith(".mp4")
							&& !fileList.get(i).getName().endsWith(".avi")
							&& !fileList.get(i).getName().endsWith(".rmvb")
							&& !fileList.get(i).getName().endsWith(".mp3")
							&& !fileList.get(i).getName().endsWith(".wma")
							&& !fileList.get(i).getName().endsWith(".txt")
							&& !fileList.get(i).getName().endsWith(".doc")) {
						file_adap.add(fileList.get(i));
					}
				}
			}

			Message msg_2 = new Message();
			msg_2.arg1 = 1;
			handler.sendMessage(msg_2);
		};

	};
	View contentView;
	ImageButton button_all;
	ImageButton button_cul;
	ImageButton button_video;
	ImageButton button_music;
	ImageButton button_doc;
	ImageButton button_other;
	PopupWindow popupWindow;

	private void showPopupWindow(final View view) {
		if (contentView == null) {
			contentView = LayoutInflater.from(getApplicationContext()).inflate(
					R.layout.popupwindow, null);
			popupWindow = new PopupWindow(contentView,
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);

			button_all = (ImageButton) contentView.findViewById(R.id.but_1);
			button_cul = (ImageButton) contentView.findViewById(R.id.but_2);
			button_video = (ImageButton) contentView.findViewById(R.id.but_3);
			button_music = (ImageButton) contentView.findViewById(R.id.but_4);
			button_doc = (ImageButton) contentView.findViewById(R.id.but_5);
			button_other = (ImageButton) contentView.findViewById(R.id.but_6);

		}
		ViewGroup parent = (ViewGroup) view.getParent();
		if (parent != null) {
			parent.removeView(contentView);
		}

		button_all.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				button_all.setImageResource(R.drawable.clear_all_check);
				button_cul.setImageResource(R.drawable.clear_cul);
				button_video.setImageResource(R.drawable.clear_video);
				button_music.setImageResource(R.drawable.clear_music);
				button_doc.setImageResource(R.drawable.clear_doc);
				button_other.setImageResource(R.drawable.clear_other);
				position = 0;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
			}
		});
		button_cul.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				button_all.setImageResource(R.drawable.clear_all);
				button_cul.setImageResource(R.drawable.clear_cul_check);
				button_video.setImageResource(R.drawable.clear_video);
				button_music.setImageResource(R.drawable.clear_music);
				button_doc.setImageResource(R.drawable.clear_doc);
				button_other.setImageResource(R.drawable.clear_other);
				position = 1;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
			}
		});
		button_video.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				button_all.setImageResource(R.drawable.clear_all);
				button_cul.setImageResource(R.drawable.clear_cul);
				button_video.setImageResource(R.drawable.clear_video_check);
				button_music.setImageResource(R.drawable.clear_music);
				button_doc.setImageResource(R.drawable.clear_doc);
				button_other.setImageResource(R.drawable.clear_other);
				position = 2;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
			}
		});
		button_music.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				button_all.setImageResource(R.drawable.clear_all);
				button_cul.setImageResource(R.drawable.clear_cul);
				button_video.setImageResource(R.drawable.clear_video);
				button_music.setImageResource(R.drawable.clear_music_check);
				button_doc.setImageResource(R.drawable.clear_doc);
				button_other.setImageResource(R.drawable.clear_other);
				position = 3;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
			}
		});
		button_doc.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				button_all.setImageResource(R.drawable.clear_all);
				button_cul.setImageResource(R.drawable.clear_cul);
				button_video.setImageResource(R.drawable.clear_video);
				button_music.setImageResource(R.drawable.clear_music);
				button_doc.setImageResource(R.drawable.clear_doc_check);
				button_other.setImageResource(R.drawable.clear_other);
				position = 4;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
			}
		});
		button_other.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				button_all.setImageResource(R.drawable.clear_all);
				button_cul.setImageResource(R.drawable.clear_cul);
				button_video.setImageResource(R.drawable.clear_video);
				button_music.setImageResource(R.drawable.clear_music);
				button_doc.setImageResource(R.drawable.clear_doc);
				button_other.setImageResource(R.drawable.clear_other_check);
				position = 5;
				new Thread(thread_spin).start();
				popupWindow.dismiss();
				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;

			}
		});
		popupWindow.setTouchable(true);

		popupWindow.setTouchInterceptor(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {

				iv_open.setImageResource(R.drawable.clear_show_up);
				isOpen = true;
				return false;
			}
		});

		popupWindow.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.clear_solid_color));
		popupWindow.showAsDropDown(view);
	}

	private void findViewId() {
		iv_open = (ImageView) findViewById(R.id.iv_open);
		jiazai_rela = (RelativeLayout) findViewById(R.id.jiazai_rela);
		popu_rela = (RelativeLayout) findViewById(R.id.popu_rela);
		RelativeLayout_sure = (RelativeLayout) findViewById(R.id.RelativeLayout_sure);
		RelativeLayout_sure.setOnClickListener(this);
		RelativeLayout_delect = (RelativeLayout) findViewById(R.id.RelativeLayout_delect);
		text_total = (TextView) findViewById(R.id.text_total);
		listadapter = (MyListView) findViewById(R.id.list_bigfile);
		listadapter.getFooterViewsCount();
		listadapter.setOnCreateContextMenuListener(this);
		listadapter.setOnRefreshListener(this);
		but_all = (Button) findViewById(R.id.but_all);
		popup = (Button) findViewById(R.id.popup_item);
		but_delect = (Button) findViewById(R.id.but_delect);
		but_no = (Button) findViewById(R.id.but_no);
		but_yes = (Button) findViewById(R.id.but_yes);
		but_yes.setOnClickListener(this);
		but_no.setOnClickListener(this);
		but_delect.setOnClickListener(this);
		but_all.setOnClickListener(this);

	}

	Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.arg1) {
			case 0:

				adapter = new BigFileAdapter(getApplicationContext(), file_adap);
				listadapter.setAdapter(adapter);
				listadapter.onRefreshComplete();
				popup.setText((file_adap.size() + getResources().getText(
						R.string.bigfile).toString()));
				jiazai_rela.setVisibility(View.INVISIBLE);
				popu_rela.setVisibility(View.VISIBLE);
				break;
			case 1:
				choose = 0;
				but_all.setText(choose + "");
				total = 0;
				text_total.setVisibility(View.INVISIBLE);
				popup.setText((file_adap.size() + getResources().getText(
						R.string.bigfile).toString()));
				adapter = new BigFileAdapter(getApplicationContext(), file_adap);
				listadapter.setAdapter(adapter);

				// adapter.notifyDataSetChanged();

				break;
			}
		};
	};

	Thread thread_scan = new Thread() {
		public void run() {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				
				File dataFile = Environment.getDataDirectory(); //  /data
				File downloadFile = Environment.getDownloadCacheDirectory(); // /cache
				File rootFile = Environment.getRootDirectory(); //  /system
				File extrenalFile = Environment.getExternalStorageDirectory(); // /storage/emulated/0
				
				
				Log.i("jingyi", "datafile="+dataFile.getAbsolutePath()
				+ " downloadFile="+downloadFile.getAbsolutePath()
				+" rootFile="+rootFile.getAbsolutePath()
				+" extrenalFile="+extrenalFile.getAbsolutePath());
				
				File imagesDir = new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()
						.toString());
				selectFile(imagesDir, 5);//MAX_DEPTH
//				File path = new File("/storage/sdcard0");
//				selectSDFile(path);
//				fileList.addAll(fileSDList);
			} else {
				File imagesDir = new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()
						.toString());
				selectFile(imagesDir, 5);
			}
			
			queryThumbnailsFile();
			
			//queryLogFile();
			
			//queryTotalCache();
//			computeCacheSize(getApplicationContext());

			file_adap = new ArrayList<File>();
			file_adap = fileList;
			Message msg = new Message();
			msg.arg1 = 0;
			handler.sendMessage(msg);

		};
	};
	
	//add liujingyi start
	
	private void queryThumbnailsFile() {
		
		File storageFile = new File(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				.toString());
		File thumbnailsFile = new File(storageFile.getAbsolutePath()+"/DCIM/.tmfs");
		Log.i("jingyi", "queryThumbnailsFile path="+thumbnailsFile.getAbsolutePath());
		if (thumbnailsFile.exists()) {
			File[] filelist = thumbnailsFile.listFiles();
			for (File file : filelist) {
				Log.i("jingyi", "file="+file.getAbsolutePath());
			}
		}
	}
	
	private void queryLogFile() {
		String totalSize = "";
		long size = 0;
		
		for(int i = 0; i < mLogPath.length; i++) {
			File file = new File(mLogPath[i]);
			
			if (file.exists() ) {
				size = getDirSize(file);
				//Log.i("jingyi", "file="+file.getAbsolutePath());
			} else {
				//Log.i("jingyi", "file="+file.getAbsolutePath()+" exit="+file.exists()+" read="+file.canRead()+" writh="+file.canWrite());
			}
			
//			long a = file.getUsableSpace();
//			long b = file.getTotalSpace();
//			long c = file.getFreeSpace();
			//Log.i("jingyi", "name="+ file.getAbsolutePath() +" size="+getSizeStr(size)+" usable="+getSizeStr(a)+" total="+getSizeStr(b)+" free="+getSizeStr(c));
			size = 0;
		}
		
		//upgradeRootPermission(getPackageCodePath());
		try {
			java.lang.Process process = null;
			
			process = Runtime.getRuntime().exec("su");
		} catch (Exception e) {
			// TODO: handle exception
		}

		
		for (File f : sytemLogFileList) {
			try {
				String name = f.getAbsolutePath();
				boolean ret = f.delete();
				Log.i("jingyi", "name="+name+" ret="+ret);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	}

	public long getDirSize(File file) {
		long size = 0;
		try {
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory()) {
					size = size + getDirSize(fileList[i]);
				} else {
					size = size + fileList[i].length();
					sytemLogFileList.add(fileList[i]);
				}
			}
		} catch (Exception e) {
			Log.i("jingyi", "error---"+e.getMessage());
		}
		return size;
	}
	
	public String computeCacheSize(Context context) {
		long fileSize = 0;
		String cacheSize = "0KB";
		File filesDir = getFilesDir();
		File cacheDir = getCacheDir();
		
		Log.i("jingyi", "computeCacheSize");
		
		//fileSize += getDirSize(filesDir);
		//fileSize += getDirSize(cacheDir);
		
		//if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {\
			//File externalCacheDir = context.getExternalCacheDir();
			//fileSize += getDirSize(externalCacheDir);
		//}
		
			PackageManager mPackageManager = context.getPackageManager();
//			List<ApplicationInfo> apps = mPackageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES 
//					| mPackageManager.GET_ACTIVITIES);
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> apps = mPackageManager.queryIntentActivities(
					mainIntent, 0);
			String pkgName = "";
			String datadir = "";
			for (ResolveInfo info : apps) {
				
				pkgName = info.activityInfo.applicationInfo.packageName;
				datadir = info.activityInfo.applicationInfo.dataDir;
				
				File file = new File(datadir);
				long size = getDirSize(file);
				fileSize += size;
				
				Log.i("jingyi", "pkgname="+pkgName+" datadir="+datadir+" size="+size);
			}
			
			
		if (fileSize > 0) {
			cacheSize = MyUtil.formatFileSize(fileSize);
			cacheSize = Formatter.formatFileSize(getApplicationContext(), fileSize);
		}
		Log.i("jingyi", "cacheSize="+cacheSize);
		return cacheSize;
	}
	
//	private void clearAppCache(Context context) {
//		@SuppressWarnings("deprecation")
//		File file = Cache
//	}
	
	private void queryTotalCache() {
		if (mPackageManager == null) {
			mPackageManager = getPackageManager();
		}
		
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		mAppEntrys = mPackageManager.queryIntentActivities(
				mainIntent, 0);
		
		long fileSize = 0;
		String cacheSize = "0KB";
		String pkgName = "";
		String datadir = "";
		for (ResolveInfo info : mAppEntrys) {
			
			pkgName = info.activityInfo.applicationInfo.packageName;
			datadir = info.activityInfo.applicationInfo.dataDir;
			
			Log.i("jingyi", "pkgName="+pkgName);
			//queryPkgCacheSize(pkgName);
			
//			File file = new File(datadir);
//			long size = getDirSize(file);
//			fileSize += size;
			
			//Log.i("jingyi", "pkgname="+pkgName+" datadir="+datadir+" size="+size);
		}
		
	}
	
	private void queryPkgCacheSize(String pkgName) {
		if (!TextUtils.isEmpty(pkgName)) {
//			if (mPackageManager == null) {
//				
//			}
			
			Log.i("jingyi", "queryPkgCacheSize");
			try {
				String strGetPackageSizeInfo = "getPackageSizeInfo";
				
//				Method getPackageSizeInfo = mPackageManager.getClass()
//						.getDeclaredMethod(strGetPackageSizeInfo, String.class,
//								IPackageStatsObserver.class);
                Method getPackageSizeInfo = mPackageManager.getClass().getMethod(
                        "getPackageSizeInfo",
                        new Class[] { String.class, IPackageStatsObserver.class });
                
				//getPackageSizeInfo.invoke(mPackageManager, pkgName, mStatsObserver);
			} catch (Exception e) {
				Log.e("jingyi", "load size fail: " + e);
			}
		}
	}

//	final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
//		public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
//			//synchronized (mEntrie) {
//				Log.i("jingyi", "cachesize="+stats.cacheSize);
//		//	}
//		}
//	};
	
    private void init() {
        if (!mAppEntrys.isEmpty()) {
        	mAppEntrys.clear();
        }

        if (!mEntriesMap.isEmpty()) {
            mEntriesMap.clear();
        }

        if (mThread.isAlive()) {
            mThread.interrupt();
        }
        
		Intent queryIntent = new Intent(Intent.ACTION_MAIN);
		queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		mAppEntrys = mPackageManager.queryIntentActivities(
				queryIntent, PackageManager.MATCH_ALL);
		Collections.sort(mAppEntrys, new ResolveInfo.DisplayNameComparator(
				mPackageManager));
		
        int index = 0;
        final int appSize = mAppEntrys.size();
        for (int i = 0; i < appSize; i++) {
            ResolveInfo info = mAppEntrys.get(i);
            
            APPInfo entry = new APPInfo(info);
            entry.flag = info.activityInfo.flags;
            entry.isSystemAPP = ((info.activityInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0);
            //ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA
            //ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
            
            mEntriesMap.put(info.activityInfo.packageName, entry);
            
//            Log.i("jingyi", "init packagename="+info.activityInfo.packageName);

//            if (info != null) {
//                mOriginalState.add(info.isVisible);
//            }
        }
        
        mHandler.sendEmptyMessage(BackgroundHandler.MSG_LOAD_SIZE);
    }
	
    class BackgroundHandler extends Handler {
        static final int MSG_LOAD_SIZE = 0x0005;

        final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                synchronized (mEntriesMap) {
                    boolean sizeChanged = false;
                    APPInfo entry = mEntriesMap.get(stats.packageName);
                    
                    if (entry != null) {
                        long externalCodeSize = stats.externalCodeSize + stats.externalObbSize;
                        long externalDataSize = stats.externalDataSize + stats.externalMediaSize
                                + stats.externalCacheSize;
                        long newSize = externalCodeSize + externalDataSize
                                + getTotalInternalSize(stats);
                        
                        if (entry.mSize != newSize || entry.mSizeInfo.mCacheSize != stats.cacheSize
                                || entry.mSizeInfo.mCodeSize != stats.codeSize
                                || entry.mSizeInfo.mDataSize != stats.dataSize
                                || entry.mSizeInfo.mExternalCodeSize != externalCodeSize
                                || entry.mSizeInfo.mExternalDataSize != externalDataSize
                                || entry.mSizeInfo.mExternalCacheSize != stats.externalCacheSize) {
                        	
                            entry.mSize = newSize;
                            entry.mSizeInfo.mCacheSize = stats.cacheSize;
                            entry.mSizeInfo.mCodeSize = stats.codeSize;
                            entry.mSizeInfo.mDataSize = stats.dataSize;
                            entry.mSizeInfo.mExternalCodeSize = externalCodeSize;
                            entry.mSizeInfo.mExternalDataSize = externalDataSize;
                            entry.mSizeInfo.mExternalCacheSize = stats.externalCacheSize;
                            entry.mInternalSize = getTotalInternalSize(stats);
                            entry.mExternalSize = getTotalExternalSize(stats);
                            sizeChanged = true;
                        }
                        
                        // Update size info
                        entry.mSizeStr = getSizeStr(entry.mSize);
                        entry.mInternalSizeStr = getSizeStr(entry.mInternalSize);
                        entry.mExternalSizeStr = getSizeStr(entry.mExternalSize);

                        if (sizeChanged) {
                            // Update the listview item text
                            //notifyDataChanged();
                        }
                        // Query another one if needed
                        Message msg = mHandler.obtainMessage(MSG_LOAD_SIZE);
                        mHandler.sendMessage(msg);
                    }
                } 
            }
        };

        BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_SIZE:
                    synchronized (mEntriesMap) {
                        String pkgName = requestComputePkgSize();
                        if (pkgName != null) {
                        	if(mPackageManager == null) {
                        		mPackageManager = getPackageManager();
                        	}
                            try {
                                Method getPackageSizeInfo = mPackageManager.getClass().getMethod(
                                    "getPackageSizeInfo",
                                    new Class[] { String.class, IPackageStatsObserver.class });
                                getPackageSizeInfo.invoke(mPackageManager,
                                    new Object[] { pkgName, mStatsObserver });
                            } catch (Exception e) {
                                Log.e("jingyi", "load size fail: " + e);
                            }
                        } else {
                        	loadSizeInfoFinished();
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        String requestComputePkgSize() {
            final int appSize = mAppEntrys.size();
            for (int i = 0; i < appSize; i++) {
                String pkgName = mAppEntrys.get(i).activityInfo.packageName;
                
                if (pkgName != null) {
                    APPInfo entry = mEntriesMap.get(pkgName);
                    if (entry != null && entry.mSize == APPInfo.SIZE_UNKNOWN) {
                        // Initilized size to be shown for user
                        entry.mSize = 0;
                        return pkgName;
                    }
                }
            }
            return null;
        }
    }
    
    private ClearCacheObserver mClearCacheObserver;
    private void loadSizeInfoFinished() {
    	
    	if (mClearCacheObserver == null) {
    	    mClearCacheObserver = new ClearCacheObserver();
    	}
    	
    	for (int i = 0; i < mAppEntrys.size(); i++) {
    		String pkgName = mAppEntrys.get(i).activityInfo.packageName;
            if (pkgName != null) {
                APPInfo entry = mEntriesMap.get(pkgName);
                Log.i("jingyi", "pkgName="+pkgName+" \n"+
                " size="+entry.mSizeStr+" InternalSize="+entry.mInternalSizeStr+" ExternalSize="+entry.mExternalSizeStr
                +" issystemapp="+entry.isSystemAPP);
            }
            
            mPackageManager.deleteApplicationCacheFiles(pkgName, mClearCacheObserver);
		}
    	
//    	List<Integer> rList;
//    	ResolveInfo info;
//    	String string = info.activityInfo.packageName;
//    	for (int i = 0; i < rList.size(); i++) {
//    		Log.i("jingyi", "open with packagename="+info.activityInfo.packageName);
//    	}
    	
    }
    
    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
//            final Message msg = mHandler.obtainMessage(MSG_CLEAR_CACHE);
//            msg.arg1 = succeeded ? OP_SUCCESSFUL : OP_FAILED;
//            mHandler.sendMessage(msg);
        	
        	Log.i("jingyi", "onRemoveCompleted packageName="+packageName+" succeeded="+succeeded);
        	
        }
    }

    private String getSizeStr(long size) {
        if (size >= 0) {
            return Formatter.formatFileSize(this, size);
        }
        return null;
    }

    private long getTotalInternalSize(PackageStats ps) {
        if (ps != null) {
            return ps.codeSize + ps.dataSize;
        }
        return APPInfo.SIZE_INVALID;
    }

    private long getTotalExternalSize(PackageStats ps) {
        if (ps != null) {
            return ps.externalCodeSize + ps.externalDataSize + ps.externalMediaSize
                    + ps.externalObbSize;
        }
        return APPInfo.SIZE_INVALID;
    }
    
   private class ApkInfo {
    	public ApkInfo() {
			// TODO Auto-generated constructor stub
		}
    	
    	Drawable apk_icon;
    	String mPackageName;
    	String mAbsolutepath;
    	String mVersionName;
    	int mVersionCode;
    	int mType;
    	
    }
    
   private int doType(PackageManager pm, String packageNmae, int versionCode) {
	   List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
	   for (PackageInfo pi : packageInfos) {
		   String pi_packageName = pi.packageName;
		   int pi_versionCode = pi.versionCode;
		   if (packageNmae.endsWith(pi_packageName)) {
			   if (versionCode == pi_versionCode) {
				   return INSTALLED;
			   } else if(versionCode > pi_versionCode) {
				   return INSTALLED_UPDATE;
			   }
		   }
	   }
	   return UNINSTALLED;
   }
   
	//add liujingyi end

	public ArrayList<File> selectSDFile(File file, int depth) {
		File[] files = file.listFiles();

		depth--;
		
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					if (depth >= 0) {
						selectSDFile(f, depth);
					}

				} else {
					if (f.length() > 10 * 1024 * 1024) {
						fileSDList.add(f);
					}
				}
			}
		}

		return fileSDList;
	}

	public ArrayList<File> selectFile(File file, int depth) {
		File[] files = file.listFiles();

		//Log.i("jingyi", "selectFile");
		depth--;
		
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {

					if (depth >= 0) {
						selectFile(f, depth);
					}

				} else {
					
					if (f.length() > select_size * 1024 * 1024) {
						fileList.add(f);
					}
					// add liujingyi start
					String name = f.getName();
					String path = f.getAbsolutePath();
					
					if (name.toLowerCase().endsWith(".apk")) {
						if (mPackageManager == null) {
							mPackageManager = getPackageManager();
						}
						PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
						
						ApkInfo apkInfo = new ApkInfo();
						if (packageInfo != null) {
							ApplicationInfo appInfo = packageInfo.applicationInfo;
							apkInfo.apk_icon = appInfo.loadIcon(mPackageManager);
							apkInfo.mPackageName = packageInfo.packageName;
							apkInfo.mAbsolutepath = path;
							apkInfo.mVersionName = packageInfo.versionName;
							apkInfo.mVersionCode = packageInfo.versionCode;
							apkInfo.mType = doType(mPackageManager, packageInfo.packageName, apkInfo.mVersionCode);
						} else {
							apkInfo.mType = APK_ERROR;
						}
					} else if (name.toLowerCase().trim().endsWith(".log") ||
							   name.toLowerCase().trim().endsWith("log.txt") ||
							   name.toLowerCase().trim().endsWith("_log")) {
						logFileList.add(f);
					}
					//add liujingyi end
				}
			}
		}

		return fileList;
	}

	private void totalFile(int p) {
		if (BigFileAdapter.isSelected.get(p)) {
			double num = (double) file_adap.get(p).length();
			total += num;
			choose += 1;
			but_all.setText(choose + "");
		} else {
			double num = (double) file_adap.get(p).length();
			total -= num;
			choose -= 1;
			but_all.setText(choose + "");
		}

	}

	private void delectfile() {

		remove_file.clear();
		Set<Integer> keyset = BigFileAdapter.isSelected.keySet();
		Iterator<Integer> it = keyset.iterator();
		while (it.hasNext()) {
			int k = (Integer) it.next();
			Boolean v = BigFileAdapter.isSelected.get(k);
			if (v.equals(true)) {
				remove_file.add(file_adap.get(k));

				Log.i("123", fileList.size() + " qqq");
				Log.i("123", file_adap.size() + " rrr");
				Log.i("123", BigFileAdapter.isSelected.size() + " www");
				Log.i("123", file_adap.get(k).getName() + " yyy");
				Log.i("123", remove_file.size() + " ttt");

				File file_d = new File(file_adap.get(k).getAbsolutePath());

				file_d.delete();
				String s = file_adap.get(k).getName();
				remove_s.add(s);
				// file_adap.remove(k);
				BigFileAdapter.isSelected.put(k, false);

			}
		}
		for (int i = 0; i < remove_file.size(); i++) {
			int j = 0;
			BigFileAdapter.isSelected.remove(remove_file.get(i - j));
			j++;
		}
		file_adap.removeAll(remove_file);
		for (int i = 0; i < BigFileAdapter.isSelected.size(); i++) {
			BigFileAdapter.isSelected.put(i, false);
		}

		for (int j = 0; j < remove_s.size(); j++) {
			for (int i = 0; i < fileList.size(); i++) {

				if (fileList.get(i).getName().equals(remove_s.get(j))) {
					fileList.remove(i);
				}
			}
		}
		remove_s.clear();

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.but_delect:

			RelativeLayout_delect.setVisibility(v.GONE);
			RelativeLayout_sure.setVisibility(v.VISIBLE);

			break;
		case R.id.but_all:
			if (b == false) {
				total = 0;

				for (int i = 0; i < file_adap.size(); i++) {
					double num = (double) file_adap.get(i).length();
					total = total + num;
					BigFileAdapter.isSelected.put(i, true);

				}
				adapter.notifyDataSetChanged();
				b = true;
				text_total.setVisibility(View.INVISIBLE);

				choose = 0;
				choose = file_adap.size();
				but_all.setText(choose + "");
				but_delect.setEnabled(true);
			} else if (b == true) {
				total = 0;
				for (int i = 0; i < file_adap.size(); i++) {
					BigFileAdapter.isSelected.put(i, false);

				}
				but_delect.setEnabled(false);
				adapter.notifyDataSetChanged();
				b = false;
				text_total.setVisibility(View.INVISIBLE);
				choose = 0;
				but_all.setText(choose + "");
			}
			break;
		case R.id.but_no:
			RelativeLayout_sure.setVisibility(v.GONE);
			RelativeLayout_delect.setVisibility(v.VISIBLE);
			break;
		case R.id.but_yes:
			delectfile();
			total = 0;
			text_total.setVisibility(v.INVISIBLE);
			choose = 0;
			but_all.setText(choose + "");
			Log.i("123", file_adap.size() + " 999999");
			adapter = new BigFileAdapter(getApplicationContext(), file_adap);
			listadapter.setAdapter(adapter);
			popup.setText((file_adap.size() + getResources().getText(
					R.string.bigfile).toString()));
			RelativeLayout_sure.setVisibility(v.GONE);
			RelativeLayout_delect.setVisibility(v.VISIBLE);
			but_delect.setEnabled(false);
			break;
		case R.id.RelativeLayout_sure:

			break;
		}
	}

	public void sdReveicer() {

		jiazai_rela.setVisibility(View.VISIBLE);
		popu_rela.setVisibility(View.INVISIBLE);
		fileList = new ArrayList<File>();
		fileSDList = new ArrayList<File>();
		contentView = null;
		// holder1.button_all.setImageResource(R.drawable.all_check);
		// holder1.button_cul.setImageResource(R.drawable.cul);
		// holder1.button_video.setImageResource(R.drawable.video);
		// holder1.button_music.setImageResource(R.drawable.music);
		// holder1.button_doc.setImageResource(R.drawable.doc);
		// holder1.button_other.setImageResource(R.drawable.other);
		total = 0;
		text_total.setVisibility(View.INVISIBLE);
		choose = 0;
		but_all.setText(choose + "");
		new Thread(thread_scan).start();
	}

	@Override
	public void onRefresh() {
		sdReveicer();

	}

}
