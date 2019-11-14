package com.android.gphonemanager;

import com.android.gphonemanager.applock.AppLockService;
import com.android.gphonemanager.applock.LockApp.State;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class GPhoneManagerService extends Service {

	IGPhoneManagerService.Stub stub = new IGPhoneManagerService.Stub() {
		
		@Override
		public boolean isPackageLocked(String packageName) throws RemoteException {
			if (AppLockService.lockedAppsMap != null) {
				if (AppLockService.lockedAppsMap.containsKey(packageName)) {
					if (AppLockService.lockedAppsMap.get(packageName).state
							== State.LOCKED) {
						return true;
					} else {
						return false;
					}
				}
			}
			return false;
		}
	};
	
	@Override
	public IBinder onBind(Intent intent) {
		return stub;
	}

}
