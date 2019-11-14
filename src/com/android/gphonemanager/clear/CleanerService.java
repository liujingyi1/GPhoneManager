package com.android.gphonemanager.clear;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.security.auth.login.LoginException;

import com.android.gphonemanager.clear.ScanType.ApkInfo;
import com.android.gphonemanager.clear.ScanType.FileInfo;

import android.R.integer;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.text.format.Formatter;
import android.util.Log;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.storage.VolumeInfo;
import android.provider.MediaStore;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Audio.Media;

public class CleanerService extends Service {
	
	public static final String ACTION_CLEAN_AND_EXIT = "com.android.gphonemanager.clear.CLEAN_AND_EXIT";
	private static final String TAG = "CleanerService";
	
    private static final int CORE_POOL_SIZE = 3;
    private static final int MAX_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_TIME = 10; // 10 seconds
    private static final int FIG_FILE_SIZE = 20;
    
	private Context mContext;
	
	private OnActionListener mOnActionListener;
	
	private CleanerServiceBinder mBinder = new CleanerServiceBinder();
	
	private boolean isScaning = false;
	
	Handler mMainHandler;
	
	private static final String[] SKIP_PACKAGES = {"com.android.gphonemanager",};
	
    public interface ProcessorCompleteListener {
        public void onProcessorCompleted(Integer type);
        public void onProcessorUpdate();
    }
	
//	private final ExecutorService mExecutorService = createThreadPool(CORE_POOL_SIZE);
	private final ExecutorService mExecutorService = Executors.newCachedThreadPool();
	
	private ConcurrentHashMap<Integer, ProcessorBase> mProcessors;
	
	private static final int MSG_UPDATE_INFO = 1000;
	private static final int MSG_SCAN_COMPLETE = 1001;
	private ScanType data;
	
	public static interface OnActionListener {
		public void onScanStarted(Context context);
		public void onScanProgressUpdated(Context context, List<String> apps);
		public void onScanInfoUpdated(Context context);
		public void onScanCompleted(Context context, int type);
		public void onCleanStarted(Context context);
		public void onCleanCompleter(Context context, long achaSize);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		Log.i("jingyi", "service onCreate");
		mContext = getApplicationContext();
		
		mProcessors = new ConcurrentHashMap<Integer, ProcessorBase>();
		
		mMainHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_UPDATE_INFO:
					mOnActionListener.onScanInfoUpdated(mContext);
					break;
				case MSG_SCAN_COMPLETE: {
					int type = msg.arg1;
					//mOnActionListener.onScanCompleted(mContext, type, data);
					
					switch (type) {
						case ScanType.ACTION_APPUNINSTALL:
							mOnActionListener.onScanCompleted(mContext, type);
							break;
						case ScanType.ACTION_BIGFILE:
							mOnActionListener.onScanCompleted(mContext, type);
							break;
						case ScanType.ACTION_MEDIA:
						case ScanType.ACTION_PHOTO:
							mOnActionListener.onScanCompleted(mContext, type);
							break;
						case ScanType.ACTION_CACHE:
							if (!mProcessors.containsKey(ScanType.PROCESSOR_APP) &&
									!mProcessors.containsKey(ScanType.PROCESSOR_FILE)) {
								mOnActionListener.onScanCompleted(mContext, type);
							}
							break;
						default:
							break;
						}
						
						if (checkStopService()) {
							isScaning = false;
						}
					}
				default:
					break;
				}
			}
		};
	}
	
	
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		Log.i("jingyi", "service start");
//		ScanAppProcessor scanAppProcessor = new ScanAppProcessor();
//		mExecutorService.execute(scanAppProcessor);
		
		
		return START_NOT_STICKY;
	}
	
	public boolean onScaning() {
		return isScaning && !mExecutorService.isShutdown();
	}
	
    public void setOnActionListener(OnActionListener listener) {
        mOnActionListener = listener;
    }
    
    public void setData(ScanType data) {
    	synchronized (mProcessorDataLock) {
    		this.data = data;
    	}
    }
    
    public ScanType getData() {
    	synchronized (mProcessorDataLock) {
			ScanType tempData = data;
			return tempData;
		}
    }
	
    ScanAppProcessor scanAppProcessor;
    ScanFileProcessor scanFileProcessor;
    ScanMediaProcessor scanMediaProcessor;
    
	public void startAllScan() {
		scanAppProcessor = new ScanAppProcessor(ScanType.PROCESSOR_APP,mProcessorListener);
		mProcessors.put(ScanType.PROCESSOR_APP, scanAppProcessor);
		mExecutorService.execute(scanAppProcessor);

		
		scanFileProcessor = new ScanFileProcessor(ScanType.PROCESSOR_FILE,mProcessorListener);
		mProcessors.put(ScanType.PROCESSOR_FILE, scanFileProcessor);
		mExecutorService.execute(scanFileProcessor);
		
		scanMediaProcessor = new ScanMediaProcessor(ScanType.PROCESSOR_MEIDA,mProcessorListener);
		mProcessors.put(ScanType.PROCESSOR_MEIDA, scanMediaProcessor);
		mExecutorService.execute(scanMediaProcessor);
		
//		ScanUninstallProcessor scanUninstallProcessor = new ScanUninstallProcessor(ScanType.PROCESSOR_APPUNINSTALL,mProcessorListener);
//		mProcessors.put(ScanType.PROCESSOR_APPUNINSTALL, scanUninstallProcessor);
//		mExecutorService.execute(scanUninstallProcessor);
		
		isScaning = true;
		mOnActionListener.onScanStarted(mContext);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("jingyi", "service onBind");
		return mBinder;
	}
	
	public class CleanerServiceBinder extends Binder {
		public CleanerService getService() {
			return CleanerService.this;
		}
	}
	
    private ExecutorService createThreadPool(int initPoolSize) {
        return new ThreadPoolExecutor(initPoolSize, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
    }
    
    private boolean checkStopService() {
    	
    	Log.i("jingyi", "checkStopService size="+mProcessors.size());
    	if(mProcessors.size() == 0) {
    		return true;
    	}
    	
        return false;
    }
    
    public void onAllProcessorsFinished() {
        Log.d(TAG, "[onAllProcessorsFinished]...");
//        stopSelf();
        mExecutorService.shutdown();
    }
    
    
    
    private class ScanMediaProcessor extends ProcessorBase {
    	
    	PackageManager mPackageManager = getPackageManager();
    	
        public ScanMediaProcessor(int type, ProcessorCompleteListener listener) {
			this.mListener = listener;
			this.type = type;
		}
    	
    	@Override
    	public void doWork() {

    		FileCategoryHelper fileCategoryHelper = new FileCategoryHelper(mContext);
    		
    		//load video & audio
    		Cursor cursor = fileCategoryHelper.query(FileCategoryHelper.FileCategory.Video, 
    	 			FileCategoryHelper.SortMethod.date);
    		if (cursor != null) {
        	 	while (cursor.moveToNext()) {
        	 		
        	 		long id = cursor.getLong(0);
        	 		String path = cursor.getString(1);
        	 		long fileSize = cursor.getLong(2);
        	 		long modifyData = cursor.getLong(3);
        	 		String name = cursor.getString(4);
        	 		
        	 		FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_VIDEO);
        	 		fileInfo.dbId = id;
        	 		fileInfo.lastModify = modifyData;
    				synchronized (mProcessorDataLock) {
    					data.mediaList.mVideoList.add(fileInfo);
    					data.mediaList.videoSize += fileSize;
    					data.mediaList.size += fileSize;
    					data.totalSize += fileSize;
    				}
    			}
        	 	cursor.close();
    		}
    		
    	 	cursor = fileCategoryHelper.query(FileCategoryHelper.FileCategory.Music, 
    	 			FileCategoryHelper.SortMethod.date);
    		if (cursor != null) {
        	 	while (cursor.moveToNext()) {
        	 		
        	 		long id = cursor.getLong(0);
        	 		String path = cursor.getString(1);
        	 		long fileSize = cursor.getLong(2);
        	 		long modifyData = cursor.getLong(3);
        	 		String name = cursor.getString(4);
        	 		
        	 		FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_AUDIO);
        	 		fileInfo.dbId = id;
        	 		fileInfo.lastModify = modifyData;
    				synchronized (mProcessorDataLock) {
    					data.mediaList.mAudioList.add(fileInfo);
    					data.mediaList.audioSIze += fileSize;
    					data.mediaList.size += fileSize;
    					data.totalSize += fileSize;
    				}
    			}
        	 	cursor.close();
    		}
    		
			synchronized (mProcessorDataLock) {
		            Message msg2 = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
		            msg2.arg1 = ScanType.ACTION_MEDIA;
		            mMainHandler.sendMessage(msg2);
			}
			//----------
            
			
			//load photo
    	 	 cursor = fileCategoryHelper.query(FileCategoryHelper.FileCategory.Picture, 
    	 			FileCategoryHelper.SortMethod.date);
    		if (cursor != null) {
        	 	while (cursor.moveToNext()) {
        	 		
        	 		long id = cursor.getLong(FileCategoryHelper.COLUMN_ID);
        	 		String path = cursor.getString(FileCategoryHelper.COLUMN_PATH);
        	 		long fileSize = cursor.getLong(FileCategoryHelper.COLUMN_SIZE);
        	 		long modifyData = cursor.getLong(FileCategoryHelper.COLUMN_DATE);
        	 		String name = cursor.getString(FileCategoryHelper.COLUMN_NAME);
        	 		Calendar calendar = Calendar.getInstance();
        	 		calendar.setTime(new Date(modifyData*1000));
        	 		
        	 		FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_PHOTO);
        	 		fileInfo.dbId = id;
        	 		fileInfo.lastModify = modifyData;
    				synchronized (mProcessorDataLock) {
    					data.picList.mPictureList.add(fileInfo);
    					data.picList.size += fileSize;
    					data.totalSize += fileSize;
    				}
    			}
        	 	cursor.close();
        	 	
        	 	SimilarPhoto.calculateFingerPrint(mContext, data.picList.mPictureList);
    		}
    		//-------
			
            mDone = true;
            processorCompleted();
    		
			 synchronized (mProcessorDataLock) {
		            Message msg1 = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
		            msg1.arg1 = ScanType.ACTION_PHOTO;
		            mMainHandler.sendMessage(msg1);
			 }
    	}
    	
    }
    
    private class ScanFileProcessor extends ProcessorBase {
    	private StorageManager mStorageManager;
    	private VolumeInfo mInternalVolume;
    	private VolumeInfo mExternalVolume;
    	private SharedPreferences preferences;
    	private PackageManager mPackageManager = getPackageManager();

        public ScanFileProcessor(int type, ProcessorCompleteListener listener) {
			this.mListener = listener;
			this.type = type;
		}
    	
		@Override
		public void doWork() {
			mStorageManager = mContext.getSystemService(StorageManager.class);
			getStorageVolumes();
			
//			File imagesDir = new File(Environment
//					.getExternalStorageDirectory().getAbsolutePath()
//					.toString());
			
		
			Log.i("jingyi", "ScanFileProcessor start mInternalVolume="+mInternalVolume);
			
			if (mInternalVolume != null) {
				
				File imagesDir = Environment.getExternalStorageDirectory();
				
				selectFile(imagesDir, -1);
			}
			
			if (mExternalVolume != null) {
				File file = mExternalVolume.getPath();
				
				selectFile(file, -1);
		    }
			
			onLoadFileFinished();
		}
		
		//depth 扫描深度，-1为没有限制，0为只扫描当前目录
		private ArrayList<File> selectFile(File file, int depth) {
			File[] files = file.listFiles();
			
			depth--;
			
			if (files != null) {
				
				synchronized (mProcessorDataLock) {
					data.nowScaningPath = file.getAbsolutePath();
					mListener.onProcessorUpdate();
				}
				
				for (File f : files) {
					if (f.isDirectory()) {
						if (depth != -1) {
							selectFile(f, depth);
						}

					} else {
						// add liujingyi start
						String name = f.getName();
						String path = f.getAbsolutePath();
						long fileSize = f.length();
						
						if (name.toLowerCase().endsWith(".apk")) {
							if (mPackageManager == null) {
								mPackageManager = getPackageManager();
							}
							PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);
							
							ApkInfo apkInfo = new ApkInfo();
							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_APK);
							if (packageInfo != null) {
								
								
								
								ApplicationInfo appInfo = packageInfo.applicationInfo;
//								apkInfo.apk_icon = appInfo.loadIcon(mPackageManager);
//								apkInfo.mLabel = (String)appInfo.loadLabel(mPackageManager);
								
								//android 中一个bug，要加上这个参数才能得到图标
								appInfo.publicSourceDir = path;
								appInfo.sourceDir = path;
								apkInfo.apk_icon = appInfo.loadIcon(mPackageManager);
								apkInfo.mLabel = (String)appInfo.loadLabel(mPackageManager);
								
								apkInfo.mPackageName = packageInfo.packageName;
								apkInfo.mVersionName = packageInfo.versionName;
								apkInfo.mVersionCode = packageInfo.versionCode;
								apkInfo.mAbsolutepath = path;
								apkInfo.mType = doType(mPackageManager, packageInfo.packageName, apkInfo.mVersionCode);
							} else {
								apkInfo.mType = ScanType.APK_ERROR;
							}
							apkInfo.size = fileSize;
							apkInfo.fileInfo = fileInfo;
							
							if (apkInfo.mType == ScanType.APK_INSTALLED ||
									apkInfo.mType == ScanType.APK_ERROR) {
								apkInfo.isCheck = true;
							}

							synchronized (mProcessorDataLock) {
								data.junkList.mAPKList.add(apkInfo);
								data.junkList.apkSize += fileSize;
								data.junkList.size += fileSize;
								data.totalSize += fileSize;
							}
						} else if (name.toLowerCase().trim().endsWith(".log") ||
								   name.toLowerCase().trim().endsWith("log.txt") ||
								   name.toLowerCase().trim().endsWith("_log")) {
							
							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_LOG);
							fileInfo.lastModify = f.lastModified();
							fileInfo.isCheck = true;
							
							synchronized (mProcessorDataLock) {
								data.junkList.mLogList.add(fileInfo);
								data.junkList.logSize += fileSize;
								data.junkList.size += fileSize;
								data.totalSize += fileSize;
							}
						} else if (MediaFileUtil.isImageFileType(name)) {
							
//							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_PHOTO);
//							
//							synchronized (mProcessorDataLock) {
//								data.picList.mPictureList.add(fileInfo);
//								data.picList.size += fileSize;
//								data.totalSize += fileSize;
//							}
						} else if(MediaFileUtil.isAudioFileType(name)) {
							
//							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_AUDIO);
//							
//							synchronized (mProcessorDataLock) {
//								data.mediaList.mAudioList.add(fileInfo);
//								data.mediaList.audioSIze += fileSize;
//								data.mediaList.size += fileSize;
//								data.totalSize += fileSize;
//							}
						} else if (MediaFileUtil.isVideoFileType(name)) {
							
//							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_VIDEO);
//							
//							synchronized (mProcessorDataLock) {
//								data.mediaList.mVideoList.add(fileInfo);
//								data.mediaList.videoSize += fileSize;
//								data.mediaList.size += fileSize;
//								data.totalSize += fileSize;
//							}
						} else if (fileSize > FIG_FILE_SIZE * 1024 * 1024) {
							
							FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_FILE);
							
							synchronized (mProcessorDataLock) {
								data.bigFileList.mBigFileList.add(fileInfo);
								data.bigFileList.size += fileSize;
								data.totalSize += fileSize;
							}
						} else {
							long time = f.lastModified();
							long now = System.currentTimeMillis();
							long day = 1000 * 60 * 60 * 24;
							if (fileSize == 0) {
								
								FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_FILE);
								
								synchronized (mProcessorDataLock) {
									data.junkList.mEmptyList.add(fileInfo);
									data.totalSize += fileSize;
								}
//							} else if (((now - time) / day) > 90) {
//								FileInfo fileInfo = new FileInfo(path, name, fileSize, ScanType.FILE_TYPE_OLD_FILE);
//								
//								synchronized (mProcessorDataLock) {
//									data.junkList.mOldList.add(fileInfo);
//									data.totalSize += fileSize;
//									data.junkList.size += fileSize;
//								}
							} else {
								synchronized (mProcessorDataLock) {
									data.usedSize += fileSize;
								}
							}
						}
						//add liujingyi end
					}
				}
			}

//			return fileList;
			return null;
		}
		
		private void onLoadFileFinished() {
			 synchronized (mProcessorDataLock) {
				 
		            mDone = true;
		            processorCompleted();
				 
	            Message msg = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
	            msg.arg1 = ScanType.ACTION_BIGFILE;
	            mMainHandler.sendMessage(msg);
	            
	            Message msg3 = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
	            msg3.arg1 = ScanType.ACTION_CACHE;
	            mMainHandler.sendMessage(msg3);
			 }
		}
		
		private void getStorageVolumes() {
	        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
	        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
	        
	        for (VolumeInfo vol : volumes) {
	            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
	                if (vol.isMountedReadable()) {
	                	mInternalVolume = vol;
	                	
//	                    final File path = mInternalVolume.getPath();
//	                    long privateUsedBytes = path.getTotalSpace() - path.getFreeSpace();
//	                    long privateTotalBytes = path.getTotalSpace();
//	                    Log.i("jingyi", "private path="+path.getAbsolutePath()+
//	                    		" used="+Formatter.formatFileSize(mContext, privateUsedBytes)+
//	                    		" total="+Formatter.formatFileSize(mContext, privateTotalBytes));
	                }
	                
	            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
	                if (vol.isMountedReadable()) {
	                	mExternalVolume = vol;
	                	
//	                    final File path = mExternalVolume.getPath();
//	                    
//	                    long publicUsedBytes = path.getTotalSpace() - path.getFreeSpace();
//	                    long publicTotalBytes = path.getTotalSpace();
//	                    Log.i("jingyi", "public path="+path.getAbsolutePath()+
//	                    		" used="+Formatter.formatFileSize(mContext, publicUsedBytes)+
//	                    		" total="+Formatter.formatFileSize(mContext, publicTotalBytes));
	                }
	            }
	        }
	        
		}
		
		   private int doType(PackageManager pm, String packageNmae, int versionCode) {
			   List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
			   for (PackageInfo pi : packageInfos) {
				   String pi_packageName = pi.packageName;
				   int pi_versionCode = pi.versionCode;
				   if (packageNmae.endsWith(pi_packageName)) {
					   if (versionCode == pi_versionCode) {
						   return ScanType.APK_INSTALLED;
					   } else if(versionCode > pi_versionCode) {
						   return ScanType.APK_INSTALLED_UPDATE;
					   }
				   }
			   }
			   return ScanType.APK_UNINSTALLED;
		   }
    }
    
    
    
    private class ScanAppProcessor extends ProcessorBase {
    	
        private volatile boolean mCanceled;
        private volatile boolean mDone;
        private HandlerThread mThread;
        private ScanAppHandler mHandler;
        
        private HashMap<String, APPInfo> mEntriesMap = new HashMap<String, APPInfo>();
        List<ResolveInfo> mAppResolveEntrys = new ArrayList<ResolveInfo>();
        private PackageManager mPackageManager = getPackageManager();
        final UserManager userManager = getSystemService(UserManager.class);
        List<UserInfo> currentProfiles;
        
        public ScanAppProcessor(int type, ProcessorCompleteListener listener) {
        	this.type = type;
			this.mListener = listener;
		}

        @Override
        public void doWork() {
	        currentProfiles = userManager.getEnabledProfiles(
	        		ActivityManager.getCurrentUser());
	        
	        for (int i = 0; i < currentProfiles.size(); i++) {
				Log.i("jingyi", "userid="+currentProfiles.get(i).id);
			}
			
	        mThread = new HandlerThread("ScanAppProcessor.worker", Process.THREAD_PRIORITY_BACKGROUND);
	        mThread.start();
	        mHandler = new ScanAppHandler(mThread.getLooper());
	        init();
	        
	        
	        
//			PackageManager packageManager = getPackageManager();
//			packageManager.getpackagesi
        }
		
	    private void init() {
	        if (!mAppResolveEntrys.isEmpty()) {
	        	mAppResolveEntrys.clear();
	        }

	        if (!mEntriesMap.isEmpty()) {
	            mEntriesMap.clear();
	        }

	        if (mThread.isAlive()) {
	            mThread.interrupt();
	        }
	        
			Intent queryIntent = new Intent(Intent.ACTION_MAIN);
			queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			mAppResolveEntrys = mPackageManager.queryIntentActivities(
					queryIntent, PackageManager.MATCH_ALL);
			
//            final List<ApplicationInfo> apps = mPackageManager.getInstalledApplications(
//                    PackageManager.GET_UNINSTALLED_PACKAGES
//                    | PackageManager.GET_DISABLED_COMPONENTS);
//            for (int i = 0; i < apps.size(); i++) {
//				Log.i("jingyi", "packagename="+apps.get(i).packageName);
//			}
            
			Collections.sort(mAppResolveEntrys, new ResolveInfo.DisplayNameComparator(
					mPackageManager));
			
	        final int appSize = mAppResolveEntrys.size();
	        
	        for (int i = appSize-1; i >= 0; i--) {
	            ResolveInfo info = mAppResolveEntrys.get(i);

	            APPInfo entry = new APPInfo(info);
	            entry.flag = info.activityInfo.flags;
	            
	            entry.isSystemAPP = ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
	            entry.isAllowClearData = ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA) != 0);
	            entry.isInRom = ((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0);
	            entry.iconBitmap = info.loadIcon(mPackageManager);
	            entry.label = (String)info.loadLabel(mPackageManager);
	            //ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA
	            //ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
	            
	            
	            if (!mEntriesMap.containsKey(info.activityInfo.packageName) && 
	            		!shouldSkip(info.activityInfo.packageName)) {
	                mEntriesMap.put(info.activityInfo.packageName, entry); 
	            } else {
	            	mAppResolveEntrys.remove(i);
	            }
	            

//	            if (info != null) {
//	                mOriginalState.add(info.isVisible);
//	            }
	        }
	        
	        
	        mHandler.sendEmptyMessage(ScanAppHandler.MSG_LOAD_SIZE);
	    }
    	
	    
	    class ScanAppHandler extends Handler {
	        static final int MSG_LOAD_SIZE = 0x0005;

	        final IPackageStatsObserver.Stub mStatsObserver = new IPackageStatsObserver.Stub() {
	            public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
	                synchronized (mEntriesMap) {
	                    boolean sizeChanged = false;
	                    APPInfo entry = mEntriesMap.get(stats.packageName);
	                    
	                    if (entry != null) {
	                        long externalCodeSize = stats.externalCodeSize + stats.externalObbSize;
	                        long externalDataSize = stats.externalDataSize + stats.externalMediaSize;
	                                //+ stats.externalCacheSize;
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
	                        
							 synchronized (mProcessorDataLock) {
								data.nowScaningPath = entry.mPackageName;
								data.totalSize += (entry.mSizeInfo.mCacheSize+entry.mSizeInfo.mExternalCacheSize
										+entry.mSizeInfo.mDataSize+entry.mSizeInfo.mExternalDataSize);
								/*if (entry.isSystemAPP) {
									data.totalSize += entry.mSizeInfo.mCacheSize;
									data.totalSize += entry.mSizeInfo.mExternalCacheSize;
								} else {
									data.totalSize += entry.mSize;
								}*/
								
							}
	                       
	                       // Log.i("jingyi", "getpackagesize pacagename="+ stats.packageName+" size="+entry.mSizeStr+" internalSize="+entry.mInternalSizeStr);

	                        if (sizeChanged) {
	                            // Update the listview item text
	                            //notifyDataChanged();
	                        	//mEntriesMap.put(stats.packageName, entry);
	                        }
	                        // Query another one if needed
	                        Message msg = mHandler.obtainMessage(MSG_LOAD_SIZE);
	                        mHandler.sendMessage(msg);
	                    }
	                } 
	            }
	        };

	        ScanAppHandler(Looper looper) {
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
	                            	
	                            	mListener.onProcessorUpdate();
//	                            	mPackageManager.getPackageSizeInfoAsUser(pkgName, currentProfiles.get(0).id, mStatsObserver);
	                            	
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
	            int appSize = mAppResolveEntrys.size();
	            for (int i = 0; i < appSize; i++) {
	                String pkgName = mAppResolveEntrys.get(i).activityInfo.packageName;
	                
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
		
	    private void loadSizeInfoFinished() {
	    	
	    	int appSize = 0;
	    	int dataSize = 0;
	    	int cacheSize = 0;
	    	
	    	int uninstallAppSize = 0;
	    	
	    	
            int appCount = mAppResolveEntrys.size();
            
            synchronized (mProcessorDataLock) {
	            for (int i = 0; i < appCount; i++) {
	                String pkgName = mAppResolveEntrys.get(i).activityInfo.packageName;
	                
	                if (pkgName != null) {
	                    APPInfo entry = mEntriesMap.get(pkgName);
	                    
	                    	appSize += entry.mSize;
	                    	dataSize += entry.mSizeInfo.mDataSize;
	                    	dataSize += entry.mSizeInfo.mExternalDataSize;
	                    	cacheSize += entry.mSizeInfo.mCacheSize;
	                    	cacheSize += entry.mSizeInfo.mExternalCacheSize;
	                    	
	                    	entry.isCheck = false;
	                    	entry.isCacheCheck = true;
	                    	
	                    	data.appInfoList.mAppInfoList.add(entry);
	                    	
//	                    	Log.i("jingyi", "loadSizeInfoFinished lable="+entry.label+" issystem="+entry.isSystemAPP);
	                    	
	                    	if (!entry.isSystemAPP) {
	                    		data.uninstallApp.mAppInfoList.add(entry);
	                    		uninstallAppSize += entry.mSize;
	                    	}
						
	                    
	                    
	                    if (entry != null && entry.mSize == APPInfo.SIZE_UNKNOWN) {
	                        // Initilized size to be shown for user
	                        entry.mSize = 0;
	                    }
	                }
	            }
	            
	            data.appInfoList.appSize = appSize;
	            data.appInfoList.cacheSize = cacheSize;
	            data.appInfoList.dataSize = dataSize;
	            
	            data.uninstallApp.size = uninstallAppSize;
	            
	            
	            mDone = true;
	            processorCompleted();
	            
	            Message msg = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
	            msg.arg1 = ScanType.ACTION_APPUNINSTALL;
	            mMainHandler.sendMessage(msg);
	            
	            Message msg3 = mMainHandler.obtainMessage(MSG_SCAN_COMPLETE);
	            msg3.arg1 = ScanType.ACTION_CACHE;
	            mMainHandler.sendMessage(msg3);
            }
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
	    
	    private String getSizeStr(long size) {
	        if (size >= 0) {
	            return Formatter.formatFileSize(mContext, size);
	        }
	        return null;
	    }
	    
		private boolean shouldSkip(String packageName) {
			for (String name : SKIP_PACKAGES) {
				if (name.equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		
    }
    
    private ProcessorCompleteListener mProcessorListener = new ProcessorCompleteListener() {
    	
       long beforeTime = System.currentTimeMillis();
    	

		@Override
		public void onProcessorCompleted(Integer type) {
			synchronized (mProcessorRemoveLock) {
				if (mProcessors.containsKey(type)) {
					mProcessors.remove(type);
					Log.i("jingyi", "onProcessorCompleted type="+type);
					if (checkStopService()) {
						onAllProcessorsFinished();
					}
				}
			}
			
		}
		
		public void onProcessorUpdate() {
			
			synchronized (mProcessorDataLock) {
				
				long time = System.currentTimeMillis();
				if ((time - beforeTime) > 50) {
					Message msg = mMainHandler.obtainMessage(MSG_UPDATE_INFO);
					mMainHandler.sendMessage(msg);
//					mMainHandler.sendMessageDelayed(msg, 10);
					beforeTime = time;
				}
			}
		};
    	
    };
    
    private final Object mProcessorRemoveLock = new Object();
    private final Object mProcessorDataLock = new Object();

}