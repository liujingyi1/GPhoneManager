package com.android.gphonemanager.applock;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class AppLockServiceDaemon extends Service {

	private static final String TAG = "Gmanager.AppLockServiceDaemon";
	
	private IApplockService mService = null;
	
	
	private ServiceConnection conn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			//mService = null;
			Log.d(TAG, "onServiceDisconnected:"+name);
			Intent intent = new Intent(AppLockServiceDaemon.this, AppLockService.class);
			startService(intent);
			bindService(intent, conn, Context.BIND_IMPORTANT);
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected:"+name);
			mService = IApplockService.Stub.asInterface(service);
			try {
				mService.getInfo();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}; 
	
	IApplockService.Stub stub = new IApplockService.Stub() {
		
		@Override
		public void getInfo() throws RemoteException {
			Log.d(TAG, "stub - getInfo");
		}
	}; 
	
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		Intent intent = new Intent(AppLockServiceDaemon.this, AppLockService.class);
		startService(intent);
		bindService(intent, conn, Context.BIND_IMPORTANT);
	};
	
	/*@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		Intent intentS = new Intent(AppLockServiceDaemon.this, AppLockService.class);
		bindService(intentS, conn, Context.BIND_IMPORTANT);
		return super.onStartCommand(intent, flags, startId);
	}*/
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind");
		return stub;
	}

}
