package com.android.gphonemanager.netmanager;

public class Constant {
        // Define loader id
	public static final int LOADER_ID_BASE = 100;
        public static final int LOADER_ID_FOR_FIREWALL = LOADER_ID_BASE + 1;
        public static final int LOADER_ID_FOR_MONTH_STATS = LOADER_ID_BASE + 2;
	public static final int LOADER_ID_FOR_DAY_STATS = LOADER_ID_BASE + 3;
        public static final int LOADER_ID_FOR_SIM_MONTH_STATS = LOADER_ID_BASE + 4;
        public static final int LOADER_ID_FOR_TOTAL_STATS = LOADER_ID_BASE + 5;

        // Define notify id
        public static final int NOTIFY_ID_BASE = 0;
        public static final int NOTIFY_ID_FOR_START = NOTIFY_ID_BASE + 1;
        public static final int NOTIFY_ID_FOR_DAY_WARNING = NOTIFY_ID_BASE + 2;
        public static final int NOTIFY_ID_FOR_MONTH_WARNING = NOTIFY_ID_BASE + 3;
        public static final int NOTIFY_ID_FOR_MONTH_LIMIT = NOTIFY_ID_BASE + 4;

        public static final int FILTER_WIFI = -1;
        public static final int FILTER_SIM1 = 0;
        public static final int FILTER_SIM2 = 1;
        public static final int FILTER_ALL = 8;

	public static final long KB_IN_BYTES = 1024;
    	public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    	public static final long GB_IN_BYTES = MB_IN_BYTES * 1024;

	/**
    	 * Sim states
    	 */
    	public static final String VALUE_ICC_ABSENT = "ABSENT";
    	public static final String VALUE_ICC_LOCKED = "LOCKED"; // pin or puk or network locked, or disable
    	public static final String VALUE_ICC_READY = "READY";
    	public static final String VALUE_ICC_NOT_READY = "NOT_READY";
    	public static final String VALUE_ICC_CARD_IO_ERROR = "CARD_IO_ERROR";
    	public static final String VALUE_ICC_UNKNOWN = "UNKNOWN";
}
