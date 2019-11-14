package com.android.gphonemanager.netmanager;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.NetworkTemplate;

import android.os.INetworkManagementService;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStatsHistory;
import android.net.INetworkManagementEventObserver;
import static android.net.NetworkStatsHistory.FIELD_RX_BYTES;
import static android.net.NetworkStatsHistory.FIELD_TX_BYTES;
import android.net.NetworkStats;
import android.net.NetworkPolicyManager;
import android.net.INetworkPolicyListener;
import static android.net.NetworkStats.SET_DEFAULT;
import static android.net.NetworkStats.SET_FOREGROUND;
import static android.net.NetworkStats.TAG_NONE;

import android.os.RemoteException;
import android.os.ServiceManager;

import android.util.Log;
import android.util.SparseArray;

import static android.net.NetworkPolicyManager.FIREWALL_CHAIN_STANDBY;
import static android.net.NetworkPolicyManager.FIREWALL_RULE_ALLOW;
import static android.net.NetworkPolicyManager.FIREWALL_RULE_DENY;

import com.android.gphonemanager.netmanager.NetRemoteException;

public class NetAdapter {

    private static final String TAG = "NetAdapter";
	
    private Context mContext;
	
    private NetworkStatsHistory mSim1History;
    private NetworkStatsHistory mSim2History;
    private NetworkStatsHistory mWifiHistory;

    private INetworkStatsService mStatsService;
    private INetworkStatsSession mSession;
    private INetworkManagementService mNetworkService;
    private NetworkPolicyManager mPolicyManager;

    private static NetAdapter mInstance;
    private SubInfoUtils mSubInfoUtils;

    public static NetAdapter getInstance(Context context) {
	Log.d(TAG, "NetAdapter [getInstance]");
	if (mInstance == null) {
	    mInstance = new NetAdapter(context);
	}
	return mInstance;
    }

    private NetAdapter(Context context) {
	mContext = context;

        mSubInfoUtils = SubInfoUtils.getInstance(context);

        mNetworkService = INetworkManagementService.Stub.asInterface(
	            ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyManager = (NetworkPolicyManager) context.getSystemService(Context.NETWORK_POLICY_SERVICE);
        try {
            mSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            Log.d(TAG, "exception happen: " + e + " , so finish current activity");
        } catch (Exception e) {
            Log.d(TAG, "exception happen: " + e);
        }
    }

    public void registerObserver(INetworkManagementEventObserver observer) {
        if (mNetworkService != null) {
            try {
                mNetworkService.registerObserver(observer);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void unregisterObserver(INetworkManagementEventObserver observer) {
        if (mNetworkService != null) {
            try {
                mNetworkService.unregisterObserver(observer);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerNetworkPolicyListener(INetworkPolicyListener policyListener) {
        if (mPolicyManager != null) {
            mPolicyManager.registerListener(policyListener);
/*
            try {
                mNetworkService.registerObserver(observer);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
*/
        }
    }

    public void unregisterNetworkPolicyListener(INetworkPolicyListener policyListener) {
        if (mPolicyManager != null) {
            mPolicyManager.unregisterListener(policyListener);
        }
    }
	
    /******************************************firewall control method***********************************/

    /**
     * Control the network to on or off
     * 
     * @param enabled
     * @throws NetRemoteException
     */
    public void setFirewallEnabled(boolean enabled) throws NetRemoteException {
	Log.d(TAG, "[setFirewallEnabled] enabled "+enabled);
        try {
            mNetworkService.setFirewallEnabled(enabled);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[setFirewallEnabled] " + e.getMessage());
        }
    }
	
    /**
     * Return true mean the network is on, false mean off
     * 
     * @return
     */
    public boolean isFirewallEnabled() {
	try {
            return mNetworkService.isFirewallEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            return false;
        }
    }
	
    /**
     * Set firewall rule for uid
     * 
     * @param uid the app id
     * @param allow
     * @throws NetRemoteException
     */
    public void setFirewallUidRule(int uid, boolean allow) throws NetRemoteException {
        int rule = allow ? FIREWALL_RULE_ALLOW : FIREWALL_RULE_DENY;
	try {
            mNetworkService.setFirewallUidRule(FIREWALL_CHAIN_STANDBY, uid, rule);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[setFirewallUidRule] " + e.getMessage());
        }
    }
	
    /**
     * Set firewall rule for uid on some network type(mobile or wifi)
     * 
     * @param uid the app id
     * @param networkType 1: wifi, other: mobile
     * @param allow
     * @throws NetRemoteException
     */
    public void setFirewallUidChainRule(int uid, 
            int networkType, boolean allow) throws NetRemoteException {
	Log.d(TAG, "[setFirewallUidChainRule] uid "+uid+",networkType "+networkType+",allow "+allow);
        try {
            mNetworkService.setFirewallUidChainRule(uid, networkType, allow);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[setFirewallUidChainRule] " + e.getMessage());
        }
    }
	
    /**
     * Clear firewall rule
     * 
     * @param networkType WIFI is 1, other MOBILE
     * @throws NetRemoteException
     */
    public void clearFirewallChain(int networkType) throws NetRemoteException {
	final String MOBILE = "mobile";
        final String WIFI = "wifi";
        final String chain = (networkType == 1) ? WIFI : MOBILE;
        try {
            mNetworkService.clearFirewallChain(chain);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[clearFirewallChain] " + e.getMessage());
        }
    }
	
	
	
    /******************************************stats method***********************************/
	
    /**
     * to force update stats
     * 
     * @throws NetRemoteException
     */
     public void forceUpdate() {
	Log.d("NetApp", "NetAdapter forceUpdate: ");
	try {
    	    mStatsService.forceUpdate();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
        }
     }
	
    /**
     * Return mobile data usage for all uids that from start to end
     * 
     * @param slotId
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public ArrayList<AppItem> getMobileSummaryForAllUid(int slotId, 
            long start, long end) throws NetRemoteException {
        String subscriberId = mSubInfoUtils.getSubscriberIdForL(mContext, slotId);
        if (subscriberId == null) {
            return null;
        }
        NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        NetworkStats stats = null;
        try {
    	    stats = mSession.getSummaryForAllUid(template, start, end, false);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getMobileSummaryForAllUid] " + e.getMessage());
        }

    	ArrayList<AppItem> items = new ArrayList<AppItem>();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        NetworkStats.Entry entry = null;
        final int size = stats != null ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);

            // Decide how to collapse items together
            final int uid = entry.uid;
            AppItem item = knownItems.get(uid);
            if (item == null) {
            	item = new AppItem();
                items.add(item);
                knownItems.put(uid, item);
            }
            item.uid = uid;
            item.total += entry.rxBytes + entry.txBytes;
        }

        return items;
    }

    /**
     * Return wifi data usage for all uids that from start to end
     * 
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public ArrayList<AppItem> getWifiSummaryForAllUid(
            long start, long end) throws NetRemoteException {
        NetworkTemplate template = NetworkTemplate.buildTemplateWifiWildcard();
        NetworkStats stats = null;
        try {
    	    stats = mSession.getSummaryForAllUid(template, start, end, false);
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getWifiSummaryForAllUid] " + e.getMessage());
        }

    	ArrayList<AppItem> items = new ArrayList<AppItem>();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        NetworkStats.Entry entry = null;
        final int size = stats != null ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);

            // Decide how to collapse items together
            final int uid = entry.uid;
            AppItem item = knownItems.get(uid);
            if (item == null) {
            	item = new AppItem();
                items.add(item);
                knownItems.put(uid, item);
            }
            item.uid = uid;
            item.total += entry.rxBytes + entry.txBytes;
        }

        return items;
    }
	
    /**
     * Return background mobile data usage for uid that from start to end
     * 
     * @param slotId
     * @param uid : the app id
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getBackgroundMobileHistoryForUid(int slotId, int uid, 
            long start, long end) throws NetRemoteException {
        String subscriberId = mSubInfoUtils.getSubscriberIdForL(mContext, slotId);
        if (subscriberId == null) {
            return 0;
        }
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        NetworkStatsHistory history = null;
        try {
             history = mSession.getHistoryForUid(
                    networkTemplate, uid, SET_DEFAULT, TAG_NONE, FIELD_RX_BYTES | FIELD_TX_BYTES);
        } catch (RemoteException e) {
            Log.w(TAG, "problem taking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getBackgroundMobileHistoryForUid] " + e.getMessage());
        }

        NetworkStatsHistory.Entry entry = null;
        long now = System.currentTimeMillis();
        entry = history.getValues(start, end, now, entry);
        final long backgroundBytes = entry.rxBytes + entry.txBytes;

	return backgroundBytes;
    }

    /**
     * Return foreground mobile data usage for uid that from start to end
     * 
     * @param slotId
     * @param uid : the app id
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getForegroundMobileHistoryForUid(int slotId, int uid, 
            long start, long end) throws NetRemoteException {
        String subscriberId = mSubInfoUtils.getSubscriberIdForL(mContext, slotId);
        if (subscriberId == null) {
            return 0;
        }
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        NetworkStatsHistory history = null;
        try {
             history = mSession.getHistoryForUid(
                    networkTemplate, uid, SET_FOREGROUND, TAG_NONE, FIELD_RX_BYTES | FIELD_TX_BYTES);
        } catch (RemoteException e) {
            Log.w(TAG, "problem taking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getForegroundMobileHistoryForUid] " + e.getMessage());
        }
        
        NetworkStatsHistory.Entry entry = null;
        long now = System.currentTimeMillis();
        entry = history.getValues(start, end, now, entry);
        final long foregroundBytes = entry.rxBytes + entry.txBytes;

	return foregroundBytes;
    }

    /**
     * Return background wifi data usage for uid that from start to end
     * 
     * @param uid : the app id
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getBackgroundWifiHistoryForUid(int uid, 
            long start, long end) throws NetRemoteException {
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        NetworkStatsHistory history = null;
        try {
             history = mSession.getHistoryForUid(
                networkTemplate, uid, SET_DEFAULT, TAG_NONE, FIELD_RX_BYTES | FIELD_TX_BYTES);
        } catch (RemoteException e) {
            Log.w(TAG, "problem taking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getBackgroundWifiHistoryForUid] " + e.getMessage());
        }
        
        NetworkStatsHistory.Entry entry = null;
        long now = System.currentTimeMillis();
        entry = history.getValues(start, end, now, entry);
        final long backgroundBytes = entry.rxBytes + entry.txBytes;

	return backgroundBytes;
    }

    /**
     * Return foreground wifi data usage for uid that from start to end
     * 
     * @param uid : the app id
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getForegroundWifiHistoryForUid(int uid, 
            long start, long end) throws NetRemoteException {
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        NetworkStatsHistory history = null;
        try {
             history = mSession.getHistoryForUid(
                    networkTemplate, uid, SET_FOREGROUND, TAG_NONE, FIELD_RX_BYTES | FIELD_TX_BYTES);
        } catch (RemoteException e) {
            Log.w(TAG, "problem taking with INetworkManagementService: " + e);
            throw new NetRemoteException("[getForegroundWifiHistoryForUid] " + e.getMessage());
        }
        
        NetworkStatsHistory.Entry entry = null;
        long now = System.currentTimeMillis();
        entry = history.getValues(start, end, now, entry);
        final long foregroundBytes = entry.rxBytes + entry.txBytes;

	return foregroundBytes;
    }
	
    /**
     * Return mobile total data usage that from start to end
     * 
     * @param slotId
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getMobileTotalBytes(int slotId, 
            long start, long end) throws NetRemoteException {
	String subscriberId = mSubInfoUtils.getSubscriberIdForL(mContext, slotId);
        if (subscriberId == null) {
            return 0;
        }
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        long totalBytes = 0l;
        try {
            if (mSession != null) {
                totalBytes = mSession.getSummaryForNetwork(networkTemplate, start, end).getTotalBytes();
            }
        } catch (RemoteException e) {
            throw new NetRemoteException(e.toString());
        }

	return totalBytes;
    }
	
    /**
     * Refresh cache history for mobile data usage.
     * After call this method, then you can call {@link #getMobileTotalBytesFromCache}
     * 
     * @param slotId
     * @return
     * @throws NetRemoteException
     */
    public void initMobileNetworkStatsHistoryCache(int slotId) throws NetRemoteException {
	String subscriberId = mSubInfoUtils.getSubscriberIdForL(mContext, slotId);
        if (subscriberId == null) {
            return;
        }
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateMobileAll(subscriberId);
        try {
            if (mSession != null) {
                if (slotId == 0) {
                    mSim1History = mSession.getHistoryForNetwork(networkTemplate, 
                            FIELD_RX_BYTES | FIELD_TX_BYTES);
                } else {
                    mSim2History = mSession.getHistoryForNetwork(networkTemplate, 
                            FIELD_RX_BYTES | FIELD_TX_BYTES);
                }
            }
        } catch (RemoteException e) {
            throw new NetRemoteException(e.toString());
        }
    }
	
    /**
     * Return mobile total data usage from cache that from start to end
     * It must be called after {@link #initMobileNetworkStatsHistoryCache}, otherwise return 0 bytes
     * 
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getMobileTotalBytesFromCache(int slot, long start, long end) throws NetRemoteException {
        NetworkStatsHistory mobileHistory = null;
        if (slot == 0) {
            mobileHistory = mSim1History;
        } else {
            mobileHistory = mSim2History;
        }
        if (mobileHistory != null) {
            long now = System.currentTimeMillis();
	    NetworkStatsHistory.Entry entry = mobileHistory.getValues(start, end, now, null);
	    final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
	    return totalBytes;
        }

        return 0l;
    }
	
    /**
     * Return wifi total data usage that from start to end
     * 
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getWifiTotalBytes(long start, long end) throws NetRemoteException {
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        long totalBytes = 0l;
        try {
            if (mSession != null) {
                totalBytes = mSession.getSummaryForNetwork(networkTemplate, start, end).getTotalBytes();
            }
        } catch (RemoteException e) {
            throw new NetRemoteException(e.toString());
        }

	return totalBytes;
    }
	
    /**
     * Refresh cache history for wifi data usage.
     * After call this method, then you can call {@link #getWifiTotalBytesFromCache}
     * 
     * @return
     * @throws NetRemoteException
     */
    public void initWifiNetworkStatsHistoryCache() throws NetRemoteException {
	NetworkTemplate networkTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        try {
            if (mSession != null) {
                mWifiHistory = mSession.getHistoryForNetwork(networkTemplate, FIELD_RX_BYTES | FIELD_TX_BYTES);
            }
        } catch (RemoteException e) {
            throw new NetRemoteException(e.toString());
        }
    }
	
    /**
     * Return wifi total data usage from cache that from start to end.
     * It must be called after {@link #initWifiNetworkStatsHistoryCache}, otherwise return 0 bytes
     * 
     * @param start : the start time
     * @param end : the end time
     * @return
     * @throws NetRemoteException
     */
    public long getWifiTotalBytesFromCache(long start, long end) throws NetRemoteException {
        if (mWifiHistory != null) {
            long now = System.currentTimeMillis();
	    NetworkStatsHistory.Entry entry = mWifiHistory.getValues(start, end, now, null);
	    final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
	    return totalBytes;
        }

        return 0l;
    }

    public void advisePersistThreshold(long thresholdBytes) {
        try {
            if (mStatsService != null) {
                mStatsService.advisePersistThreshold(thresholdBytes);
            }
        } catch (RemoteException e) {
            //throw new NetRemoteException(e.toString());
            Log.d(TAG, "RemoteException when advisePersistThreshold()");
        }
    }
	
    public static class AppItem {
    	public int uid;
    	public long total;
    }

}
