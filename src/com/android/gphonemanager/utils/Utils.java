package com.android.gphonemanager.utils;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.WindowManager;

public class Utils {
	private static final String TAG = "Gmanager.Utils";
	
	public static int getEffectiveUserId(Context context) {
		UserManager um = UserManager.get(context);
		if (um != null) {
            return um.getCredentialOwnerProfile(UserHandle.myUserId());
        } else {
            Log.e(TAG, "Unable to acquire UserManager");
            return UserHandle.myUserId();
        }
	}
	
	public static int getWindowWidth(Context context){
		WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
		int width = wm.getDefaultDisplay().getWidth();
		return width;
	}
	
	public static int getWindowHeight(Context context){
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		int height = wm.getDefaultDisplay().getHeight();
		return height;
	}
}
