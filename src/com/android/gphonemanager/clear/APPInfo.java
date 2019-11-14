package com.android.gphonemanager.clear;

import java.io.Serializable;

import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class APPInfo implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int SIZE_UNKNOWN = -1;
    public static final int SIZE_INVALID = -2;
	
    ResolveInfo mResolveInfo;
	SizeInfo mSizeInfo;
	
	String mPackageName;
	Drawable iconBitmap;
	String label;
	
    int flag = 0;
    boolean mVisibile;
    boolean mLocked;
    boolean isSystemAPP;
    boolean isAllowClearData;
    boolean isInRom;
    
    long mSize;
    long mInternalSize;
    long mExternalSize;
    String mSizeStr;
    String mInternalSizeStr;
    String mExternalSizeStr;
    
    boolean isCheck;
    boolean isCacheCheck;
    boolean isDelete;
    boolean isCacheDelete;

    APPInfo(ResolveInfo appInfo) {
        this.mResolveInfo = appInfo;
        this.mSize = SIZE_UNKNOWN;
        mSizeInfo = new SizeInfo();
        mPackageName = appInfo.activityInfo.packageName;
    }
	
    class SizeInfo implements Serializable{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		long mCacheSize;
        long mCodeSize;
        long mDataSize;
        long mExternalCodeSize;
        long mExternalDataSize;

        // This is the part of externalDataSize that is in the cache
        // section of external storage. Note that we don't just combine
        // this with cacheSize because currently the platform can't
        // automatically trim this data when needed, so it is something
        // the user may need to manage. The externalDataSize also includes
        // this value, since what this is here is really the part of
        // externalDataSize that we can just consider to be "cache" files
        // for purposes of cleaning them up in the app details UI.
        long mExternalCacheSize;
    }
	
    public Drawable getIcon() {
//        if (iconBitmap == null) {
//        	mResolveInfo.loadIcon(pm)
//        	mResolveInfo.
//            iconBitmap = LauncherAppState.getInstance().getIconCache().getIcon(intent, user);
//            Log.d(TAG, "getIcon: icon is null, get it again, app=" + this);
//        }
        return iconBitmap;
    }
}
