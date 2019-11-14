package com.android.gphonemanager.netmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.util.Log;

import com.android.gphonemanager.netmanager.TimeUtils;
import com.android.gphonemanager.netmanager.SharePreferenceUtils;

public class AppUtils {
	
	private static final String TAG = "AppUtils";
	
	private static ArrayList<AppUsage> apps = null;

	/** special application UID used to indicate "any application" */
	public static final int SPECIAL_UID_ANY = -10;

	/** special application UID used to indicate the Linux Kernel */
	public static final int SPECIAL_UID_KERNEL = -11;

	public static final int SIM1 = 0;
	public static final int SIM2 = 1;
        public static final int WIFI = -1;

	public static final int NETWORK_TYPE_MOBILE = 0;
	public static final int NETWORK_TYPE_WIFI = 1;

	public static void reset() {
		apps = null;
	}

        /**
         * When slotId=-1,=====>get wifi stats; 
         * slotId=0,get sim1 stats, slotId=1,get sim2 stats.
         */
        public static long getTotalStatsForCurrentMonth(
			Context ctx, int slotId) throws Exception {
            long[] time = TimeUtils.getTime(TimeUtils.MONTH_TYPE);
            NetAdapter netAdapter = NetAdapter.getInstance(ctx);
            if (slotId == SIM1) {
                return netAdapter.getMobileTotalBytes(SIM1, time[0], time[1]);
            } else if (slotId == SIM2) {
                return netAdapter.getMobileTotalBytes(SIM2, time[0], time[1]);
            } else {
                return netAdapter.getWifiTotalBytes(time[0], time[1]);
            }
        }

	public static ArrayList<AppUsage> getAllAppsStatsForCurrentMonth(
			Context ctx, boolean onlyUserApp) throws Exception {
		
		ArrayList<AppUsage> appDetails;
		if (onlyUserApp) {
			appDetails = getUserApps(ctx);
		} else {
			appDetails = getApps(ctx);
		}
		
		int selected_mobile[] = getUidsWithMobile(ctx);
		int selected_wifi[] = getUidsWithWifi(ctx);
		long[] time = TimeUtils.getTime(TimeUtils.MONTH_TYPE);
		// Log.d("firewall", "apputil start " + time[0] + ", end " + time[1]);
		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		ArrayList<NetAdapter.AppItem> appItemsWithMobile1 = netAdapter
				.getMobileSummaryForAllUid(SIM1, time[0], time[1]);
		ArrayList<NetAdapter.AppItem> appItemsWithMobile2 = netAdapter
				.getMobileSummaryForAllUid(SIM2, time[0], time[1]);
		ArrayList<NetAdapter.AppItem> appItemsWithWifi = netAdapter
				.getWifiSummaryForAllUid(time[0], time[1]);
		for (AppUsage app : appDetails) {
			NetAdapter.AppItem appItem = findAppItem(appItemsWithMobile1,
					app.uid);
			if (appItem != null) {
				app.sim1Total = appItem.total;
			}
			appItem = findAppItem(appItemsWithMobile2, app.uid);
			if (appItem != null) {
				app.sim2Total = appItem.total;
			}
			appItem = findAppItem(appItemsWithWifi, app.uid);
			if (appItem != null) {
				app.wifiTotal = appItem.total;
			}
			// check if this application is selected
			if (/*!app.selected_wifi && */hasUidInArray(selected_wifi, app.uid)) {
				app.selected_wifi = true;
			} else {
				app.selected_wifi = false;
			}
			if (/*!app.selected_mobile && */hasUidInArray(selected_mobile, app.uid)) {
				app.selected_mobile = true;
			} else {
				app.selected_mobile = false;
			}
			app.timeType = AppUsage.MONTH;
		}
		Collections.sort(appDetails);
		return appDetails;
	}
	
	public static ArrayList<AppUsage> getAllAppsHasStatsForOneDay(Context ctx,
			long dayStart, long dayEnd) throws NetRemoteException {
		ArrayList<AppUsage> appDetails = getApps(ctx);
		ArrayList<AppUsage> hasStatsApps = new ArrayList<AppUsage>();
		int selected_mobile[] = getUidsWithMobile(ctx);
		int selected_wifi[] = getUidsWithWifi(ctx);
		// long[] time = getTime(DAY_TYPE, date);
		Log.d(TAG, "AppUtil start " + dayStart + ", end "
				+ dayEnd);
		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		ArrayList<NetAdapter.AppItem> appItemsWithMobile1 = netAdapter
				.getMobileSummaryForAllUid(SIM1, dayStart, dayEnd);
		ArrayList<NetAdapter.AppItem> appItemsWithMobile2 = netAdapter
				.getMobileSummaryForAllUid(SIM2, dayStart, dayEnd);
		ArrayList<NetAdapter.AppItem> appItemsWithWifi = netAdapter
				.getWifiSummaryForAllUid(dayStart, dayEnd);
		for (AppUsage app : appDetails) {
			NetAdapter.AppItem appItem = findAppItem(appItemsWithMobile1,
					app.uid);
			AppUsage appDetail = null;
			if (appItem != null) {
				// app.sim1Total = appItem.total;
				appDetail = new AppUsage();
				appDetail.sim1Total = appItem.total;
			}
			appItem = findAppItem(appItemsWithMobile2, app.uid);
			if (appItem != null) {
				// app.sim2Total = appItem.total;
				if (appDetail == null) {
					appDetail = new AppUsage();
				}
				appDetail.sim2Total = appItem.total;
			}
			appItem = findAppItem(appItemsWithWifi, app.uid);
			if (appItem != null) {
				// app.wifiTotal = appItem.total;
				if (appDetail == null) {
					appDetail = new AppUsage();
				}
				appDetail.wifiTotal = appItem.total;
			}
			if (appDetail == null) {
				continue;
			}
			// check if this application is selected
			if (/*!app.selected_wifi && */hasUidInArray(selected_wifi, app.uid)) {
				app.selected_wifi = true;
			} else {
				app.selected_wifi = false;
			}
			if (/*!app.selected_mobile && */hasUidInArray(selected_mobile, app.uid)) {
				app.selected_mobile = true;
			} else {
				app.selected_mobile = false;
			}

			// clone attribute from app object
			appDetail.uid = app.uid;
			appDetail.name = app.name;
			appDetail.selected_mobile = app.selected_mobile;
			appDetail.selected_wifi = app.selected_wifi;
			appDetail.icon = app.icon;
			appDetail.timeType = AppUsage.DAY;

			// add to list
			hasStatsApps.add(appDetail);
		}
		//appDetails.clear();
		Collections.sort(hasStatsApps);
		return hasStatsApps;
	}

        public static ArrayList<AppUsage> getMobileAllAppsHasStatsForOneDay(Context ctx,
			long dayStart, long dayEnd, int slotId) throws NetRemoteException {
                if (slotId != SIM1 && slotId != SIM2) return null;

		ArrayList<AppUsage> appDetails = getApps(ctx);
		ArrayList<AppUsage> hasStatsApps = new ArrayList<AppUsage>();
		int selected_mobile[] = getUidsWithMobile(ctx);
		int selected_wifi[] = getUidsWithWifi(ctx);
		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		ArrayList<NetAdapter.AppItem> appItemsWithMobile = netAdapter
				.getMobileSummaryForAllUid(slotId, dayStart, dayEnd);
		for (AppUsage app : appDetails) {
			NetAdapter.AppItem appItem = findAppItem(appItemsWithMobile, app.uid);
			AppUsage appDetail = null;
			if (appItem != null) {
				// app.sim1Total = appItem.total;
				appDetail = new AppUsage();
                                if (slotId == SIM1) {
                                    appDetail.sim1Total = appItem.total;
                                } else if (slotId == SIM2) {
                                    appDetail.sim2Total = appItem.total;
                                }
			}
			if (appDetail == null) {
				continue;
			}
			// check if this application is selected
			if (/*!app.selected_wifi && */hasUidInArray(selected_wifi, app.uid)) {
				app.selected_wifi = true;
			} else {
				app.selected_wifi = false;
			}
			if (/*!app.selected_mobile && */hasUidInArray(selected_mobile, app.uid)) {
				app.selected_mobile = true;
			} else {
				app.selected_mobile = false;
			}

			// clone attribute from app object
			appDetail.uid = app.uid;
			appDetail.name = app.name;
			appDetail.selected_mobile = app.selected_mobile;
			appDetail.selected_wifi = app.selected_wifi;
			appDetail.icon = app.icon;
			appDetail.timeType = AppUsage.DAY;

			// add to list
			hasStatsApps.add(appDetail);

		}
		//appDetails.clear();
		Collections.sort(hasStatsApps);
		return hasStatsApps;
	}

        public static ArrayList<AppUsage> getWifiAllAppsHasStatsForOneDay(Context ctx,
			long dayStart, long dayEnd) throws NetRemoteException {
		ArrayList<AppUsage> appDetails = getApps(ctx);
		ArrayList<AppUsage> hasStatsApps = new ArrayList<AppUsage>();
		int selected_mobile[] = getUidsWithMobile(ctx);
		int selected_wifi[] = getUidsWithWifi(ctx);
		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		ArrayList<NetAdapter.AppItem> appItemsWithWifi = netAdapter
				.getWifiSummaryForAllUid(dayStart, dayEnd);
		for (AppUsage app : appDetails) {
			AppUsage appDetail = null;
			NetAdapter.AppItem appItem = findAppItem(appItemsWithWifi, app.uid);
			if (appItem != null) {
				appDetail = new AppUsage();
				appDetail.wifiTotal = appItem.total;
			}
			if (appDetail == null) {
				continue;
			}
			// check if this application is selected
			if (/*!app.selected_wifi && */hasUidInArray(selected_wifi, app.uid)) {
				app.selected_wifi = true;
			} else {
				app.selected_wifi = false;
			}
			if (/*!app.selected_mobile && */hasUidInArray(selected_mobile, app.uid)) {
				app.selected_mobile = true;
			} else {
				app.selected_mobile = false;
			}

			// clone attribute from app object
			appDetail.uid = app.uid;
			appDetail.name = app.name;
			appDetail.selected_mobile = app.selected_mobile;
			appDetail.selected_wifi = app.selected_wifi;
			appDetail.icon = app.icon;
			appDetail.timeType = AppUsage.DAY;

			// add to list
			hasStatsApps.add(appDetail);
		}
		//appDetails.clear();
		Collections.sort(hasStatsApps);
		return hasStatsApps;
	}

	public static ArrayList<DayStatsInfo> getDaysStatsBeforeCurrentDay(
			Context ctx) throws NetRemoteException {
		TimeUtils.DayBound[] days = TimeUtils.getDayBoundsBeforeCurrentDay();

		ArrayList<DayStatsInfo> dayStatsInfos = new ArrayList<DayStatsInfo>();

		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		netAdapter.initMobileNetworkStatsHistoryCache(SIM1);
		netAdapter.initMobileNetworkStatsHistoryCache(SIM2);
		netAdapter.initWifiNetworkStatsHistoryCache();
		for (int i = days.length - 1; i >= 0; i--) {
                        TimeUtils.DayBound day = days[i];
			long sim1Stats = netAdapter.getMobileTotalBytesFromCache(SIM1,
					day.start, day.end);
			long sim2Stats = netAdapter.getMobileTotalBytesFromCache(SIM2,
					day.start, day.end);
			long wifiStats = netAdapter.getWifiTotalBytesFromCache(day.start,
					day.end);
                        if (sim1Stats + sim2Stats + wifiStats == 0) {
                            continue;
                        }
			DayStatsInfo info = new DayStatsInfo();
			info.start = day.start;
			info.end = day.end;
			info.date = day.date;
			info.stats = sim1Stats + sim2Stats + wifiStats;
			dayStatsInfos.add(info);
		}
		return dayStatsInfos;
	}

        public static ArrayList<DayStatsInfo> getDaysStatsBeforeCurrentDay(
			Context ctx, int slotId) throws NetRemoteException {
                if (slotId == 0) {
                    slotId = SIM1;
                } else if (slotId == 1) {
                    slotId = SIM2;
                } else {
                    return null;
                }

		TimeUtils.DayBound[] days = TimeUtils.getDayBoundsBeforeCurrentDay();

		ArrayList<DayStatsInfo> dayStatsInfos = new ArrayList<DayStatsInfo>();

		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		netAdapter.initMobileNetworkStatsHistoryCache(slotId);
		for (int i = days.length - 1; i >= 0; i--) {
                        TimeUtils.DayBound day = days[i];
			long simStats = netAdapter.getMobileTotalBytesFromCache(slotId,
					day.start, day.end);
                        if (simStats == 0) {
                            continue;
                        }
			DayStatsInfo info = new DayStatsInfo();
			info.start = day.start;
			info.end = day.end;
			info.date = day.date;
			info.stats = simStats;
			dayStatsInfos.add(info);
		}
		return dayStatsInfos;
	}

        public static ArrayList<DayStatsInfo> getDaysWifiStatsBeforeCurrentDay(
			Context ctx) throws NetRemoteException {
		TimeUtils.DayBound[] days = TimeUtils.getDayBoundsBeforeCurrentDay();

		ArrayList<DayStatsInfo> dayStatsInfos = new ArrayList<DayStatsInfo>();

		NetAdapter netAdapter = NetAdapter.getInstance(ctx);
		// netAdapter.forceUpdate();
		netAdapter.initWifiNetworkStatsHistoryCache();
		for (int i = days.length - 1; i >= 0; i--) {
                        TimeUtils.DayBound day = days[i];
			long wifiStats = netAdapter.getWifiTotalBytesFromCache(day.start,
					day.end);
                        if (wifiStats == 0) {
                            continue;
                        }
			DayStatsInfo info = new DayStatsInfo();
			info.start = day.start;
			info.end = day.end;
			info.date = day.date;
			info.stats = wifiStats;
			dayStatsInfos.add(info);
		}
		return dayStatsInfos;
	}

	private static NetAdapter.AppItem findAppItem(
			ArrayList<NetAdapter.AppItem> appItems, int uid) {
		if (appItems == null)
			return null;

		for (NetAdapter.AppItem appItem : appItems) {
			if (uid == appItem.uid) {
				return appItem;
			}
		}
		return null;
	}

	/**
	 * @param ctx
	 *            application context (mandatory)
	 * @return a list of applications
	 */
	public static ArrayList<AppUsage> getApps(Context ctx) {
		// Log.d("NetApp", "getApps() apps " + apps);
		if (apps != null) {
			// return cached instance
			return apps;
		}

		try {
			final PackageManager pkgmanager = ctx.getPackageManager();
			final List<ApplicationInfo> installed = pkgmanager
					.getInstalledApplications(0);
			final HashMap<Integer, AppUsage> map = new HashMap<Integer, AppUsage>();
			String name = null;
			String cachekey = null;
			AppUsage app = null;
			for (final ApplicationInfo apinfo : installed) {
				app = map.get(apinfo.uid);
				// filter applications which are not allowed to access the
				// Internet
				if (app == null
						&& PackageManager.PERMISSION_GRANTED != pkgmanager
								.checkPermission(Manifest.permission.INTERNET,
										apinfo.packageName)) {
					continue;
				}
				// try to get the application label from our cache -
				// getApplicationLabel() is horribly slow!!!!
				cachekey = "cache.label." + apinfo.packageName;
				name = SharePreferenceUtils.getCacheAppName(ctx, cachekey);
				if (name.length() == 0) {
					// get label and put on cache
					name = pkgmanager.getApplicationLabel(apinfo).toString();
					SharePreferenceUtils.cacheAppName(ctx, cachekey, name);
				}
				if (app == null) {
					app = new AppUsage();
					app.uid = apinfo.uid;
					app.name = name;
					app.icon = apinfo.loadIcon(pkgmanager);
					map.put(apinfo.uid, app);
				}

			}
			// add special applications to the list
			/*
			 * final AppDetail special[] = { new AppDetail( SPECIAL_UID_ANY,
			 * "(Any application) - Same as selecting all applications", false,
			 * false, null), new AppDetail(SPECIAL_UID_KERNEL,
			 * "(Kernel) - Linux kernel", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("root"),
			 * "(root) - Applications running as root", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("media"),
			 * "Media server", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("vpn"),
			 * "VPN networking", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("shell"),
			 * "Linux shell", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("gps"), "GPS", false,
			 * false, null) }; for (int i = 0; i < special.length; i++) { app =
			 * special[i]; if (app.uid != -1 && !map.containsKey(app.uid)) { //
			 * check if this application is allowed if
			 * (Arrays.binarySearch(selected_wifi, app.uid) >= 0) {
			 * app.selected_wifi = true; } if
			 * (Arrays.binarySearch(selected_mobile, app.uid) >= 0) {
			 * app.selected_mobile = true; } map.put(app.uid, app); } }
			 */
			apps = new ArrayList<AppUsage>();
			for (AppUsage application : map.values()) {
				apps.add(application);
			}

			return apps;
		} catch (Exception e) {
			// alert(ctx, "error: " + e);
		}
		return null;
	}
	
	/**
	 * @param ctx
	 *            application context (mandatory)
	 * @return a list of applications
	 */
	public static ArrayList<AppUsage> getUserApps(Context ctx) {
		// Log.d("NetApp", "getApps() apps " + apps);
		if (apps != null) {
			// return cached instance
			return apps;
		}

		try {
			final PackageManager pkgmanager = ctx.getPackageManager();
			final List<ApplicationInfo> installed = pkgmanager
					.getInstalledApplications(0);
			final HashMap<Integer, AppUsage> map = new HashMap<Integer, AppUsage>();
			String name = null;
			String cachekey = null;
			AppUsage app = null;
			for (final ApplicationInfo apinfo : installed) {
				app = map.get(apinfo.uid);
				// filter applications which are not allowed to access the
				// Internet
				if (app == null
						&& PackageManager.PERMISSION_GRANTED != pkgmanager
								.checkPermission(Manifest.permission.INTERNET,
										apinfo.packageName)) {
					continue;
				}
				
				boolean isSystemApp = ((apinfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
				if (isSystemApp) {
					continue;
				}
				
				// try to get the application label from our cache -
				// getApplicationLabel() is horribly slow!!!!
				cachekey = "cache.label." + apinfo.packageName;
				name = SharePreferenceUtils.getCacheAppName(ctx, cachekey);
				if (name.length() == 0) {
					// get label and put on cache
					name = pkgmanager.getApplicationLabel(apinfo).toString();
					SharePreferenceUtils.cacheAppName(ctx, cachekey, name);
				}
				if (app == null) {
					app = new AppUsage();
					app.uid = apinfo.uid;
					app.name = name;
					app.icon = apinfo.loadIcon(pkgmanager);
					map.put(apinfo.uid, app);
				}

			}
			// add special applications to the list
			/*
			 * final AppDetail special[] = { new AppDetail( SPECIAL_UID_ANY,
			 * "(Any application) - Same as selecting all applications", false,
			 * false, null), new AppDetail(SPECIAL_UID_KERNEL,
			 * "(Kernel) - Linux kernel", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("root"),
			 * "(root) - Applications running as root", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("media"),
			 * "Media server", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("vpn"),
			 * "VPN networking", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("shell"),
			 * "Linux shell", false, false, null), new
			 * AppDetail(android.os.Process.getUidForName("gps"), "GPS", false,
			 * false, null) }; for (int i = 0; i < special.length; i++) { app =
			 * special[i]; if (app.uid != -1 && !map.containsKey(app.uid)) { //
			 * check if this application is allowed if
			 * (Arrays.binarySearch(selected_wifi, app.uid) >= 0) {
			 * app.selected_wifi = true; } if
			 * (Arrays.binarySearch(selected_mobile, app.uid) >= 0) {
			 * app.selected_mobile = true; } map.put(app.uid, app); } }
			 */
			apps = new ArrayList<AppUsage>();
			for (AppUsage application : map.values()) {
				apps.add(application);
			}

			return apps;
		} catch (Exception e) {
			// alert(ctx, "error: " + e);
		}
		return null;
	}

	private static boolean hasUidInArray(int[] uids, int uid) {
		for (int id : uids) {
			if (id == uid) {
				return true;
			}
		}
		return false;
	}

	private static int[] getUidsWithMobile(Context context) {
		FirewallDatabaseHelper databaseHelper = new FirewallDatabaseHelper(
				context);
		Cursor cursor = databaseHelper.queryDenyUidsWithMobile();
		int[] uids = getUids(cursor);
		if (cursor != null) {
			cursor.close();
		}
		return uids;
	}

	private static int[] getUidsWithWifi(Context context) {
		FirewallDatabaseHelper databaseHelper = new FirewallDatabaseHelper(
				context);
		Cursor cursor = databaseHelper.queryDenyUidsWithWifi();
		int[] uids = getUids(cursor);
		if (cursor != null) {
			cursor.close();
		}
		return uids;
	}

	private static int[] getUids(Cursor cursor) {
		int[] uids = new int[0];
		if (cursor == null) {
			return uids;
		}
		uids = new int[cursor.getCount()];
		int i = 0;
		while (cursor.moveToNext()) {
			uids[i] = cursor.getInt(cursor
					.getColumnIndexOrThrow(FirewallDatabaseHelper.UID));
			i++;
		}
		return uids;
	}

}
