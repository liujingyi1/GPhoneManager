package com.android.gphonemanager.netmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharePreferenceUtils {

	public static final String PREFS_NAME = "rgk_net_manager_prefs";

	public static final String PREF_MOBILE_UIDS = "denyed_uids_3G";
	public static final String PREF_WIFI_UIDS = "denyed_uids_wifi";
	public static final String PREF_ENABLED = "enabled_firewall";

	public static final String STATS_MONITOR_ENABLED = "stats_monitor_enabled";

	public static final String SIM1_DAY_WARNING_ENABLED = "sim1_day_warning_enabled";
	public static final String SIM1_MONTH_WARNING_ENABLED = "sim1_month_warning_enabled";
	public static final String SIM1_MONTH_LIMIT_ENABLED = "sim1_month_limit_enabled";

	public static final String SIM2_DAY_WARNING_ENABLED = "sim2_day_warning_enabled";
	public static final String SIM2_MONTH_WARNING_ENABLED = "sim2_month_warning_enabled";
	public static final String SIM2_MONTH_LIMIT_ENABLED = "sim2_month_limit_enabled";

	public static final String SIM1_DAY_WARNING_VALUE = "sim1_day_warning_value";
	public static final String SIM1_MONTH_WARNING_VALUE = "sim1_month_warning_value";
	public static final String SIM1_MONTH_LIMIT_VALUE = "sim1_month_limit_value";

	public static final String SIM2_DAY_WARNING_VALUE = "sim2_day_warning_value";
	public static final String SIM2_MONTH_WARNING_VALUE = "sim2_month_warning_value";
	public static final String SIM2_MONTH_LIMIT_VALUE = "sim2_month_limit_value";

	/**
	 * For testing
	 * 
	 * @param ctx
	 * @return
	 */
	public static int[] getDenyedUidsWithMobile(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		String savedUids_mobile = prefs.getString(PREF_MOBILE_UIDS, "");
		int[] selected_mobile = new int[0];
		if (savedUids_mobile.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_mobile,
					"|");
			selected_mobile = new int[tok.countTokens()];
			for (int i = 0; i < selected_mobile.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_mobile[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_mobile[i] = -1;
					}
				}
			}
			// Sort the array to allow using "Arrays.binarySearch" later
			Arrays.sort(selected_mobile);
		}
		return selected_mobile;
	}

	/**
	 * For testing
	 * 
	 * @param ctx
	 * @param apps
	 */
	public static void saveDenyedUidsWithMobileAndWifi(Context ctx,
			ArrayList<AppUsage> apps) {
		if (apps == null) {
			return;
		}
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		// Builds a pipe-separated list of names
		StringBuilder newuids_wifi = new StringBuilder();
		StringBuilder newuids_mobile = new StringBuilder();
		int length = apps.size();
		for (int i = 0; i < length; i++) {
			AppUsage appDetail = apps.get(i);
			if (appDetail.selected_wifi) {
				if (newuids_wifi.length() != 0)
					newuids_wifi.append('|');
				newuids_wifi.append(appDetail.uid);
			}
			if (appDetail.selected_mobile) {
				if (newuids_mobile.length() != 0)
					newuids_mobile.append('|');
				newuids_mobile.append(appDetail.uid);
			}
		}
		// save the new list of UIDs
		Editor edit = prefs.edit();
		edit.putString(PREF_WIFI_UIDS, newuids_wifi.toString());
		edit.putString(PREF_MOBILE_UIDS, newuids_mobile.toString());
		edit.commit();
	}

	/**
	 * For testing
	 * 
	 * @param ctx
	 * @return
	 */
	public static int[] getDenyedUidsWithWifi(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		String savedUids_wifi = prefs.getString(PREF_WIFI_UIDS, "");
		int[] selected_wifi = new int[0];
		if (savedUids_wifi.length() > 0) {
			// Check which applications are allowed
			final StringTokenizer tok = new StringTokenizer(savedUids_wifi, "|");
			selected_wifi = new int[tok.countTokens()];
			for (int i = 0; i < selected_wifi.length; i++) {
				final String uid = tok.nextToken();
				if (!uid.equals("")) {
					try {
						selected_wifi[i] = Integer.parseInt(uid);
					} catch (Exception ex) {
						selected_wifi[i] = -1;
					}
				}
			}
			// Sort the array to allow using "Arrays.binarySearch" later
			Arrays.sort(selected_wifi);
		}
		return selected_wifi;
	}

	public static void setEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(PREF_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(PREF_ENABLED, enabled).commit();
	}

	public static boolean getEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(PREF_ENABLED, false);
	}

	public static void setSim1DayWarningValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM1_DAY_WARNING_VALUE, value).commit();
	}

	public static int getSim1DayWarningValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM1_DAY_WARNING_VALUE, 0);
	}

	public static void setSim1MonthWarningValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM1_MONTH_WARNING_VALUE, value).commit();
	}

	public static int getSim1MonthWarningValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM1_MONTH_WARNING_VALUE, 0);
	}

	public static void setSim1MonthLimitValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM1_MONTH_LIMIT_VALUE, value).commit();
	}

	public static int getSim1MonthLimitValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM1_MONTH_LIMIT_VALUE, 0);
	}

	public static void setSim2DayWarningValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM2_DAY_WARNING_VALUE, value).commit();
	}

	public static int getSim2DayWarningValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM2_DAY_WARNING_VALUE, 0);
	}

	public static void setSim2MonthWarningValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM2_MONTH_WARNING_VALUE, value).commit();
	}

	public static int getSim2MonthWarningValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM2_MONTH_WARNING_VALUE, 0);
	}

	public static void setSim2MonthLimitValue(Context ctx, int value) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putInt(SIM2_MONTH_LIMIT_VALUE, value).commit();
	}

	public static int getSim2MonthLimitValue(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getInt(SIM2_MONTH_LIMIT_VALUE, 0);
	}

	public static void setSim1DayWarningEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM1_DAY_WARNING_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM1_DAY_WARNING_ENABLED, enabled).commit();
	}

	public static boolean getSim1DayWarningEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM1_DAY_WARNING_ENABLED, false);
	}

	public static void setSim1MonthWarningEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM1_MONTH_WARNING_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM1_MONTH_WARNING_ENABLED, enabled).commit();
	}

	public static boolean getSim1MonthWarningEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM1_MONTH_WARNING_ENABLED, false);
	}

	public static void setSim1MonthLimitEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM1_MONTH_LIMIT_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM1_MONTH_LIMIT_ENABLED, enabled).commit();
	}

	public static boolean getSim1MonthLimitEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM1_MONTH_LIMIT_ENABLED, false);
	}

	public static void setSim2DayWarningEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM2_DAY_WARNING_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM2_DAY_WARNING_ENABLED, enabled).commit();
	}

	public static boolean getSim2DayWarningEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM2_DAY_WARNING_ENABLED, false);
	}

	public static void setSim2MonthWarningEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM2_MONTH_WARNING_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM2_MONTH_WARNING_ENABLED, enabled).commit();
	}

	public static boolean getSim2MonthWarningEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM2_MONTH_WARNING_ENABLED, false);
	}

	public static void setSim2MonthLimitEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		if (prefs.getBoolean(SIM2_MONTH_LIMIT_ENABLED, false) == enabled) {
			return;
		}
		prefs.edit().putBoolean(SIM2_MONTH_LIMIT_ENABLED, enabled).commit();
	}

	public static boolean getSim2MonthLimitEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(SIM2_MONTH_LIMIT_ENABLED, false);
	}

	public static boolean getStatsMonitorEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getBoolean(STATS_MONITOR_ENABLED, true);
	}

	public static void setStatsMonitorEnabled(Context ctx, boolean enabled) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		/*
		if (prefs.getBoolean(STATS_MONITOR_ENABLED, false) == enabled) {
			return;
		}
		*/
		prefs.edit().putBoolean(STATS_MONITOR_ENABLED, enabled).commit();
	}

	public static void cacheAppName(Context ctx, String key, String name) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		prefs.edit().putString(key, name).commit();
	}

	public static String getCacheAppName(Context ctx, String key) {
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		return prefs.getString(key, "");
	}

}
