package com.android.gphonemanager;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

public class PhoneManagerProviderPublic extends ContentProvider {
	private static final String TAG = "Gmanager.PhoneManagerProviderPublic";

	public static final String AUTHORITIES = "com.gmanager.app.public";
	public static final String TABLE_SETTINGS = "settings";
	public static final String TABLE_APP_LOCK = "applock";
	public static final String TABLE_FREEZE_APPS = "freeze";

	public static final int SETTINGS = 1;
	public static final int SETTINGS_ITEM = 2;
	private static final int APP_LOCK = 3;
	private static final int APP_LOCK_ITEM = 4;
	public static final int FREEZE = 5;
	public static final int FREEZE_ITEM = 6;

	public static final String APP_LOCK_IS_LOCKED = "locked";
	public static final String APP_LOCK_PACKAGE = "package";
	public static final String APP_LOCK_CLASS = "class";

	public static final String SETTINGS_NAME = "settings_name";
	public static final String SETTINGS_VALUE = "settings_value";
	
	public static final String FREEZE_ID = "_id";
	public static final String FREEZE_PACKAGE_NAME = "package_name";
	public static final String FREEZE_UID = "uid";

	private DBHelper dbHelper;
	private static UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITIES, TABLE_SETTINGS, SETTINGS);
		uriMatcher.addURI(AUTHORITIES, TABLE_SETTINGS + "/#", SETTINGS_ITEM);
		uriMatcher.addURI(AUTHORITIES, TABLE_APP_LOCK, APP_LOCK);
		uriMatcher.addURI(AUTHORITIES, TABLE_APP_LOCK + "/#", APP_LOCK_ITEM);
		uriMatcher.addURI(AUTHORITIES, TABLE_FREEZE_APPS, FREEZE);
		uriMatcher.addURI(AUTHORITIES, TABLE_FREEZE_APPS + "/#", FREEZE_ITEM);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.w(TAG, "delete: ");
		return -1;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			return AUTHORITIES + "/" + TABLE_SETTINGS;
		case SETTINGS_ITEM:
			return AUTHORITIES + "/" + TABLE_SETTINGS;
		case APP_LOCK:
			return AUTHORITIES + "/" + TABLE_APP_LOCK;
		case APP_LOCK_ITEM:
			return AUTHORITIES + "/" + TABLE_APP_LOCK;
		case FREEZE:
			return AUTHORITIES + "/" + TABLE_FREEZE_APPS;
		case FREEZE_ITEM:
			return AUTHORITIES + "/" + TABLE_FREEZE_APPS;
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.w(TAG, "insert: " + values + "\nuri: " + uri);
		return null;
	}

	@Override
	public boolean onCreate() {
		Log.d(TAG, "onCreate");
		dbHelper = new DBHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Log.d(TAG, "query");
		SQLiteDatabase database = dbHelper.getReadableDatabase();
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			Log.d(TAG, "SETTINGS");
			return database.query(TABLE_SETTINGS, projection, selection,
					selectionArgs, null, null, sortOrder);
			
		case SETTINGS_ITEM: {
			Log.d(TAG, "SETTINGS_ITEM");
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}
			
			return database.query(TABLE_SETTINGS, projection, where,
					selectionArgs, null, null, sortOrder);
		}
		case APP_LOCK:
			Log.d(TAG, "APP_LOCK");
			return database.query(TABLE_APP_LOCK, projection, selection,
					selectionArgs, null, null, sortOrder);

		case APP_LOCK_ITEM: {
			Log.d(TAG, "APP_LOCK_ITEM");
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}

			return database.query(TABLE_APP_LOCK, projection, where,
					selectionArgs, null, null, sortOrder);
		}
		case FREEZE:
			Log.d(TAG, "FREEZE");
			return database.query(TABLE_FREEZE_APPS, projection, selection,
					selectionArgs, null, null, sortOrder);

		case FREEZE_ITEM: {
			Log.d(TAG, "FREEZE_ITEM");
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}

			return database.query(TABLE_FREEZE_APPS, projection, where,
					selectionArgs, null, null, sortOrder);
		}
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		Log.w(TAG, "update: ");
		return -1;
	}

	

}
