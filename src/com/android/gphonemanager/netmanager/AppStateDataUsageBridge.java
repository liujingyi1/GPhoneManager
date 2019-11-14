/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.gphonemanager.netmanager;

import com.android.gphonemanager.netmanager.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import android.content.Context;
import android.text.format.Formatter;

import java.util.ArrayList;

public class AppStateDataUsageBridge extends AppStateBaseBridge {

    private static final String TAG = "AppStateDataUsageBridge";

    private final DataSaverBackend mDataSaverBackend;
    NetAdapter mFirewallService = null;
    Context mContext;
	boolean sim1Enable = false;
	boolean sim2Enable = false;
    
    public AppStateDataUsageBridge(ApplicationsState appState, Callback callback,
            DataSaverBackend backend, NetAdapter service, Context context) {
        super(appState, callback);
        mDataSaverBackend = backend;
        mFirewallService = service;
        mContext = context;
        
		SubInfoUtils mSubInfoUtils;
		mSubInfoUtils = SubInfoUtils.getInstance(mContext);
		int[] slot = mSubInfoUtils.getActivatedSlotList();
		for (int i : slot) {
			sim1Enable = (i == 0) ? true : sim1Enable;
			sim2Enable = (i == 1) ? true : sim2Enable;
		}
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = mAppSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry app = apps.get(i);
            DataUsageState extraInfo= new DataUsageState(mDataSaverBackend.isWhitelisted(app.info.uid),
                    mDataSaverBackend.isBlacklisted(app.info.uid));
            
            try {
    			long[] time = TimeUtils.getTime(TimeUtils.MONTH_TYPE);
    			if (sim1Enable) {
    				long backgroundData = mFirewallService.
    						getBackgroundMobileHistoryForUid(0, app.info.uid, time[0], time[1]);
    				extraInfo.sim1BackData = backgroundData;
    			} else {
    				extraInfo.sim1BackData = 0;
    			}
    			if (sim2Enable) {
    				long backgroundData = mFirewallService.
    						getBackgroundMobileHistoryForUid(1, app.info.uid, time[0], time[1]);
    				extraInfo.sim2BackData = backgroundData;
    			} else {
    				extraInfo.sim2BackData = 0;
    			}
    			
    			extraInfo.wifiBackData = mFirewallService.
    					getBackgroundWifiHistoryForUid(app.info.uid, time[0], time[1]); 
    			
    			extraInfo.totalBackData = extraInfo.sim1BackData+extraInfo.sim2BackData+
    					              extraInfo.wifiBackData;
			} catch (Exception e) {
			}

			app.extraInfo = extraInfo;
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry app, String pkg, int uid) {
        app.extraInfo = new DataUsageState(mDataSaverBackend.isWhitelisted(uid),
                mDataSaverBackend.isBlacklisted(uid));
    }

    public static class DataUsageState implements Comparable<DataUsageState> {
        public boolean isDataSaverWhitelisted;
        public boolean isDataSaverBlacklisted;
        long wifiBackData;
        long sim1BackData;
        long sim2BackData;
        long totalBackData;

        public DataUsageState(boolean isDataSaverWhitelisted, boolean isDataSaverBlacklisted) {
            this.isDataSaverWhitelisted = isDataSaverWhitelisted;
            this.isDataSaverBlacklisted = isDataSaverBlacklisted;
        }

		@Override
		public int compareTo(DataUsageState another) {
			long left = another.totalBackData;
			long right = totalBackData;
			return left < right ? -1 : (left == right ? 0 : 1);
		}
    }
}
