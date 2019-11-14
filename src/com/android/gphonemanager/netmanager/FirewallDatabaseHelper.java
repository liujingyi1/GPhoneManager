package com.android.gphonemanager.netmanager;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FirewallDatabaseHelper extends SQLiteOpenHelper {
	
    private static final String DATABASE_NAME = "firewall.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_MOBILE = "mobile";
    private static final String TABLE_WIFI = "wifi";
	
    public static final String ID = "_id";
    public static final String APP_NAME = "app_name";
    public static final String UID = "uid";
	
    private Context mContext = null;

    public FirewallDatabaseHelper(Context context) {
	super(context, DATABASE_NAME, null, DATABASE_VERSION);
	mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
	db.execSQL("CREATE TABLE " + TABLE_MOBILE + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                //APP_NAME + " TEXT," +
                UID + " INTEGER);");
		
	db.execSQL("CREATE TABLE " + TABLE_WIFI + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                //APP_NAME + " TEXT," +
                UID + " INTEGER);");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion , int newVersion ) {
	db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOBILE);
	db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFI);

    }
	
    public Cursor queryDenyUidsWithMobile() {
	String sql = "SELECT * FROM " + TABLE_MOBILE;
	return getReadableDatabase().rawQuery(sql, null);
    }
	
    public Cursor queryDenyUidsWithWifi() {
	String sql = "SELECT * FROM " + TABLE_WIFI;
	return getReadableDatabase().rawQuery(sql, null);
    }
	
    public boolean hasUidWithMobile(int uid) {
	String sql = "SELECT * FROM " + TABLE_MOBILE + " WHERE " + UID + "=" + uid;
	Cursor c = getReadableDatabase().rawQuery(sql, null);
	boolean has = false;
	if (c != null) {
	    has = c.getCount() > 0;
	    c.close();
	}
	return has;
    }
	
    public boolean hasUidWithWifi(int uid) {
	String sql = "SELECT * FROM " + TABLE_WIFI + " WHERE " + UID + "=" + uid;
	Cursor c = getReadableDatabase().rawQuery(sql, null);
	boolean has = false;
	if (c != null) {
	    has = c.getCount() > 0;
	    c.close();
	}
	return has;
    }
    
    public int getMobileUidCount() {
    	String sql = "SELECT * FROM " + TABLE_MOBILE;
    	Cursor c = getReadableDatabase().rawQuery(sql, null);
    	if (c != null) {
    		int count = c.getCount();
    		c.close();
    	    return count;
    	}
    	return 0;
    }
    
    public int getWifiUidCount() {
    	String sql = "SELECT * FROM " + TABLE_WIFI;
    	Cursor c = getReadableDatabase().rawQuery(sql, null);
    	if (c != null) {
    		int count = c.getCount();
    		c.close();
    	    return count;
    	}
    	return 0;
    }
	
    public void insertDenyUidWithMobile(int uid) {
	getWritableDatabase().execSQL("INSERT INTO " + TABLE_MOBILE 
		+ "(" + UID + ") VALUES(" + uid + ");");
    }
	
    public void insertDenyUidWithWifi(int uid) {
	getWritableDatabase().execSQL("INSERT INTO " + TABLE_WIFI 
			+ "(" + UID + ") VALUES(" + uid + ");");
    }
	
    public void deleteDenyUidWithMobile(int uid) {
	getWritableDatabase().execSQL("DELETE FROM " + TABLE_MOBILE + " WHERE "
		+ UID + "=" + uid);
    }
	
    public void deleteDenyUidWithWifi(int uid) {
	getWritableDatabase().execSQL("DELETE FROM " + TABLE_WIFI + " WHERE "
		+ UID + "=" + uid);
    }

    public void clearDenyUidsWithMobile() {
	getWritableDatabase().execSQL("DELETE FROM " + TABLE_MOBILE);
    }
	
    public void clearDenyUidsWithWifi() {
    	getWritableDatabase().execSQL("DELETE FROM " + TABLE_WIFI);
    }

}
