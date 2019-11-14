package com.android.gphonemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
	private static final String TAG = "Gmanager.DBHelper";
	
	private static final String DATABASE_NAME = "gmanager.db";
	private static final int DATABASE_VERSION = 1;

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "DB Create");
		db.execSQL("CREATE TABLE " + PhoneManagerProvider.TABLE_SETTINGS + "("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT," + PhoneManagerProvider.SETTINGS_NAME
				+ " INTEGER," + PhoneManagerProvider.SETTINGS_VALUE + " INTEGER);");
		
		db.execSQL("CREATE TABLE " + PhoneManagerProvider.TABLE_APP_LOCK + "("
				+ "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ PhoneManagerProvider.APP_LOCK_IS_LOCKED + " INTEGER," + PhoneManagerProvider.APP_LOCK_PACKAGE
				+ " TEXT," + PhoneManagerProvider.APP_LOCK_CLASS + " TEXT);");
		
		db.execSQL("CREATE TABLE " + PhoneManagerProvider.TABLE_FREEZE_APPS + "("
				+ PhoneManagerProvider.FREEZE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + PhoneManagerProvider.FREEZE_PACKAGE_NAME
				+ " TEXT," + PhoneManagerProvider.FREEZE_UID + " INTEGER);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + PhoneManagerProvider.TABLE_SETTINGS);
		db.execSQL("DROP TABLE IF EXISTS " + PhoneManagerProvider.TABLE_APP_LOCK);
		db.execSQL("DROP TABLE IF EXISTS " + PhoneManagerProvider.TABLE_FREEZE_APPS);
		onCreate(db);
	}

}
