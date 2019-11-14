package com.android.gphonemanager.netmanager;

import java.util.ArrayList;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;

public class AppsLoader extends AsyncTaskLoader<ArrayList<AppUsage>> {

	private Bundle mExtras;

	public AppsLoader(Context context) {
		super(context);
	}

	public void setExtras(Bundle extras) {
		mExtras = extras;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		forceLoad();
	}

	@Override
	public ArrayList<AppUsage> loadInBackground() {
		long start = 0;
		long end = 0;
        int filterType = Constant.FILTER_ALL;
        boolean onlyUserApp = false;
		try {
			if (mExtras != null) {
				onlyUserApp = mExtras.getBoolean("only_user_app", false);
				
				return AppUtils.getAllAppsStatsForCurrentMonth(getContext(), onlyUserApp);
				
//				start = mExtras.getLong(OneDayAppsStatsActivity.EXTRA_START, 0);
//				end = mExtras.getLong(OneDayAppsStatsActivity.EXTRA_END, 0);
//                                filterType = mExtras.getInt(OneDayAppsStatsActivity.EXTRA_TYPE, 
//                                        Constant.FILTER_ALL);
//                                if (filterType == Constant.FILTER_ALL) {
//                                    return AppUtils.getAllAppsHasStatsForOneDay(getContext(),
//							start, end);
//                                } else if (filterType == Constant.FILTER_SIM1) {
//                                    return AppUtils.getMobileAllAppsHasStatsForOneDay(getContext(), 
//                                                        start, end, AppUtils.SIM1);
//                                } else if (filterType == Constant.FILTER_SIM2) {
//                                    return AppUtils.getMobileAllAppsHasStatsForOneDay(getContext(), 
//                                                        start, end, AppUtils.SIM2);
//                                } else if (filterType == Constant.FILTER_WIFI) {
//                                    return AppUtils.getWifiAllAppsHasStatsForOneDay(getContext(), 
//                                                        start, end);
//                                }
//                                return null;
			} else {
				return AppUtils.getAllAppsStatsForCurrentMonth(getContext(), false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
//		 return AppUtils.getApps(getContext());
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	protected void onReset() {
		super.onReset();
		cancelLoad();
	}


}
