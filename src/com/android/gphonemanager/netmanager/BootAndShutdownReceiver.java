package com.android.gphonemanager.netmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class BootAndShutdownReceiver extends BroadcastReceiver {
	
	private static final String TAG = "BootAndShutdownReceiver";

	@Override
	public void onReceive(final Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(TAG, "BootAndShutdownReceiver: [onReceive] action " + action);
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			new Thread() {

				@Override
				public void run() {
					NetAdapter firewallService = NetAdapter
							.getInstance(context);
					if (SharePreferenceUtils.getEnabled(context)) {
						try {
							firewallService.setFirewallEnabled(true);
						} catch (NetRemoteException e) {
							SharePreferenceUtils.setEnabled(context, false);
							e.printStackTrace();
						}
					}
					FirewallDatabaseHelper databaseHelper = new FirewallDatabaseHelper(
							context);
					Cursor mobileCursor = databaseHelper
							.queryDenyUidsWithMobile();
					if (mobileCursor != null) {
						while (mobileCursor.moveToNext()) {
							int uid = mobileCursor
									.getInt(mobileCursor
											.getColumnIndexOrThrow(FirewallDatabaseHelper.UID));
							try {
								firewallService.setFirewallUidChainRule(uid, 0,
										true);
							} catch (NetRemoteException e) {
								databaseHelper.deleteDenyUidWithMobile(uid);
								e.printStackTrace();
							}
						}
						mobileCursor.close();
					}

					Cursor wifiCursor = databaseHelper.queryDenyUidsWithWifi();
					if (wifiCursor != null) {
						while (wifiCursor.moveToNext()) {
							int uid = wifiCursor
									.getInt(wifiCursor
											.getColumnIndexOrThrow(FirewallDatabaseHelper.UID));
							try {
								firewallService.setFirewallUidChainRule(uid, 1,
										true);
							} catch (NetRemoteException e) {
								databaseHelper.deleteDenyUidWithWifi(uid);
								e.printStackTrace();
							}
						}
						wifiCursor.close();
					}
				}
			}.start();
			
			// start service
//			if (SharePreferenceUtils.getStatsMonitorEnabled(context)) {
//				context.startService(new Intent(context, DataUsageService.class));
//			}
		} else if (Intent.ACTION_SHUTDOWN.equals(action)) {

		}

	}

}
