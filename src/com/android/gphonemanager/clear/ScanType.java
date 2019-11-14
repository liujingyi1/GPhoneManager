package com.android.gphonemanager.clear;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import android.graphics.drawable.Drawable;
import android.util.Log;


public class ScanType implements Serializable {
	
	/**
	 * 
	 */

	private static final long serialVersionUID = 1L;
	
	public static final int ACTION_CACHE = 0;
	public static final int ACTION_APPUNINSTALL = 1;
	public static final int ACTION_BIGFILE = 2;
	public static final int ACTION_PHOTO = 3;
	public static final int ACTION_MEDIA = 4;
	
	public static final int PROCESSOR_APP = 0;
	public static final int PROCESSOR_APPUNINSTALL = 1;
	public static final int PROCESSOR_FILE = 2;
	public static final int PROCESSOR_MEIDA = 3;
	
	public static final int APK_ERROR = -1;
	public static final int APK_INSTALLED = 0;
	public static final int APK_UNINSTALLED = 1;
	public static final int APK_INSTALLED_UPDATE = 2;
	
	public static final int FILE_TYPE_APK = 1;
	public static final int FILE_TYPE_PHOTO = 2;
	public static final int FILE_TYPE_AUDIO = 3;
	public static final int FILE_TYPE_VIDEO = 4;
	public static final int FILE_TYPE_FILE = 5;
	public static final int FILE_TYPE_LOG = 6;
	public static final int FILE_TYPE_SYSTEMLOG = 7;
	public static final int FILE_TYPE_THUMB = 8;
	public static final int FILE_TYPE_OLD_FILE = 9;
	public static final int FILE_TYPE_UNKNOW = 10;
	
	public long totalSize;
	public long usedSize;
	String nowScaningPath;
	
	public JunkList junkList = new JunkList();
	public UninstallApp uninstallApp = new UninstallApp();
	public AppInfoList appInfoList = new AppInfoList();
	public MediaList mediaList = new MediaList();
	public PicList picList = new PicList();
	public BigFileList bigFileList = new BigFileList();
	
	
	class JunkList {
		ArrayList<ApkInfo> mAPKList = new ArrayList<ApkInfo>();
		ArrayList<FileInfo> mLogList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> mSystemLogList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> mThumbList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> mEmptyList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> mOldList = new ArrayList<FileInfo>();
		long apkSize;
		long logSize;
		long systemLogSize;
		long thumbSize;
		long emptySzie;
		long oldSzie;
		
		long size;
	}
	
	class UninstallApp implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ArrayList<APPInfo> mAppInfoList = new ArrayList<APPInfo>();
		long size;
	}
	
	class AppInfoList implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ArrayList<APPInfo> mAppInfoList = new ArrayList<APPInfo>();
		long appSize;
		long dataSize;
		long cacheSize;
		//long size;
	}
	
	class MediaList implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ArrayList<FileInfo> mVideoList = new ArrayList<FileInfo>();
		ArrayList<FileInfo> mAudioList = new ArrayList<FileInfo>();
		long videoSize;
		long audioSIze;
		long size;
	}
	
	class PicList implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ArrayList<FileInfo> mPictureList = new ArrayList<FileInfo>();
		long size;
	}
	
	class BigFileList implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ArrayList<FileInfo> mBigFileList = new ArrayList<FileInfo>();
		long size;
	}
	
   public static class ApkInfo {
    	Drawable apk_icon;
    	String mLabel;
    	String mPackageName;
    	String mAbsolutepath;
    	String mVersionName;
    	int mVersionCode;
    	int mType;
    	long size;
    	boolean isDelete;
    	boolean isCheck;
    	FileInfo fileInfo;
    }
   
   public static class FileInfo {
	    public FileInfo() {}
	    public FileInfo(String path, String name, long size, int type) {
	    	this.path = path;
	    	this.name = name;
	    	this.type = type;
	    	this.size = size;
	    }
	   
		String path;
		String name;
	   	int type;
	   	long size;
	   	long lastModify;
	   	boolean isDelete;
	   	boolean isCheck;
	   	
	   	boolean IsDir;
	   	boolean canRead;
	   	boolean canWrite;
	   	boolean isHidden;
	   	
	   	long finger;
	   	long dbId;  // id in the database, if is from database
	   	
	   	String tag;
   }
}
