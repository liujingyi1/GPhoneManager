package com.android.gphonemanager.netmanager;

import android.content.Context;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.telephony.TelephonyManager;

import java.util.List;
import java.util.Arrays;
import android.util.Log;
import android.os.ServiceManager;

public class SubInfoUtils {
	
	private static final String TAG = "SubInfoUtils";

    private static SubInfoUtils mSubInfoUtils;
    private SubscriptionManager mSubscriptionManager;

    public static SubInfoUtils getInstance(Context context) {
        if (mSubInfoUtils == null) {
            mSubInfoUtils = new SubInfoUtils(context);
        }

        return mSubInfoUtils;
    }

    private SubInfoUtils(Context context) {
        mSubscriptionManager = SubscriptionManager.from(context);
    }

    public List<SubscriptionInfo> getActivatedSubInfoList() {
        return mSubscriptionManager.getActiveSubscriptionInfoList();
    }

    public int getActivatedSubInfoCount() {
        return mSubscriptionManager.getActiveSubscriptionInfoCount();
    }

    public SubscriptionInfo getSubInfoUsingSlot(int slotId) {
        List<SubscriptionInfo> subInfoRecordList = getActivatedSubInfoList();
        if (subInfoRecordList != null && subInfoRecordList.size() > 0) {
            for (SubscriptionInfo subInfoRecord : subInfoRecordList) {
                if (subInfoRecord.getSimSlotIndex() == slotId) {
                    return subInfoRecord;
                }
            }
        }
        return null;
    }

    public int[] getActivatedSlotList() {
        List<SubscriptionInfo> infos = getActivatedSubInfoList();
        if (infos == null) {
            return new int[0];
        }
        int size = infos.size();
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = infos.get(i).getSimSlotIndex();
        }
        Arrays.sort(slots);
        return slots;
    }

    /*
    public static String getSubscriberId(Context context, int slot) {
        try {
            ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager
                    .getService(Context.TELEPHONY_SERVICE_EX));
            if (telephonyEx == null) {
                return null;
            }
            String subscriberId = telephonyEx.getSubscriberId(slot);
            return subscriberId;
        } catch (android.os.RemoteException ex) {
            Log.d(NetApplication.TAG, "RemoteException when get subscriber id");
            return null;
        }
    }
    */

    public String getSubscriberIdForL(Context context, int slot) {
	List<SubscriptionInfo> infos = getActivatedSubInfoList();
        if (infos == null) {
            return null;
        }
	int subId = -1;
	for (SubscriptionInfo info : infos) {
		if (info.getSimSlotIndex() == slot) {
			subId = info.getSubscriptionId();
			break;
		}
	}
	if (subId == -1) {
		return null;
	}
	TelephonyManager tele = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
	String subscriberId = tele.getSubscriberId(subId);
	return subscriberId;
    }

    public int getDefaultDataSlotId() {
        SubscriptionInfo info = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        if (info == null) {
            return -1;
        }
        return info.getSimSlotIndex();
    }

	public static boolean getDataEnabled(Context context) {
		boolean enabled = false;
		TelephonyManager tele = (TelephonyManager)context
					.getSystemService(Context.TELEPHONY_SERVICE);
		try {
            		enabled = tele.getDataEnabled();
            		Log.d(TAG, "SubInfoUtils getDataEnabled() data state=" + enabled);
        	} catch (NullPointerException e) {
			Log.d(TAG, "SubInfoUtils failed get TelephonyManager exception:" + e);
			enabled = false;
        	}
		return enabled;
	}

}
