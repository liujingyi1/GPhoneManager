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

public class PhoneManagerProvider extends ContentProvider {
	private static final String TAG = "Gmanager.FingerprintProvider";

	public static final String AUTHORITIES = "com.gmanager.app";
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
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			count = db.delete(TABLE_SETTINGS, selection, selectionArgs);
			return count;
		case SETTINGS_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			count = db.delete(TABLE_SETTINGS, where, selectionArgs);
			return count;
		}
		case APP_LOCK:
			count = db.delete(TABLE_APP_LOCK, selection, selectionArgs);
			return count;
		case APP_LOCK_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			count = db.delete(TABLE_APP_LOCK, where, selectionArgs);
			return count;
		}
		case FREEZE:
			count = db.delete(TABLE_FREEZE_APPS, selection, selectionArgs);
			return count;
		case FREEZE_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			count = db.delete(TABLE_FREEZE_APPS, where, selectionArgs);
			return count;
		}
		default:
			break;
		}
		return count;
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
		Log.i(TAG, "insert: " + values + "\nuri: " + uri);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriMatcher.match(uri)) {
		case SETTINGS: {
			long rowId = db.insert(TABLE_SETTINGS, null, values);
			if (rowId < 0) {
				throw new SQLiteException("Unable to insert " + values
						+ " for " + uri);
			}
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		case APP_LOCK: {
			long rowId = db.insert(TABLE_APP_LOCK, null, values);
			if (rowId < 0) {
				throw new SQLiteException("Unable to insert " + values
						+ " for " + uri);
			}
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		case FREEZE: {
			long rowId = db.insert(TABLE_FREEZE_APPS, null, values);
			if (rowId < 0) {
				throw new SQLiteException("Unable to insert " + values
						+ " for " + uri);
			}
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(insertUri, null);
			return insertUri;
		}
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
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
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case SETTINGS:
			count = db.update(TABLE_SETTINGS, values, selection, selectionArgs);
			return count;
		case SETTINGS_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}
			count = db.update(TABLE_SETTINGS, values, where, selectionArgs);
			return count;
		}
		case APP_LOCK:
			count = db.update(TABLE_APP_LOCK, values, selection, selectionArgs);
			return count;
		case APP_LOCK_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}
			count = db.update(TABLE_APP_LOCK, values, where, selectionArgs);
			return count;
		}
		case FREEZE:
			count = db.update(TABLE_FREEZE_APPS, values, selection, selectionArgs);
			return count;
		case FREEZE_ITEM: {
			long id = ContentUris.parseId(uri);
			String where = "_id=" + id;
			if (selection != null && !"".equals(selection)) {
				where = selection + " and " + where;
			}
			count = db.update(TABLE_FREEZE_APPS, values, where, selectionArgs);
			return count;
		}
		default:
			throw new IllegalArgumentException("Unknow Uri: " + uri);
		}
	}

	

}
