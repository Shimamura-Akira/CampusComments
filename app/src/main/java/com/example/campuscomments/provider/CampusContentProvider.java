package com.example.campuscomments.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.example.campuscomments.db.LocalDatabaseHelper;

public class CampusContentProvider extends ContentProvider {
    public static final String AUTHORITY = "com.example.campuscomments.provider";
    public static final Uri POIS_URI = Uri.parse("content://" + AUTHORITY + "/pois");

    private static final int POIS = 1;
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, "pois", POIS);
    }

    private LocalDatabaseHelper databaseHelper;

    @Override
    public boolean onCreate() {
        databaseHelper = new LocalDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (URI_MATCHER.match(uri) != POIS) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String order = sortOrder == null ? LocalDatabaseHelper.COL_NAME + " ASC" : sortOrder;
        Cursor cursor = db.query(LocalDatabaseHelper.TABLE_POI_CACHE, projection, selection, selectionArgs, null, null, order);
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        if (URI_MATCHER.match(uri) == POIS) {
            return "vnd.android.cursor.dir/vnd.com.example.campuscomments.poi";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) != POIS) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        long id = databaseHelper.getWritableDatabase().insert(LocalDatabaseHelper.TABLE_POI_CACHE, null, values);
        Uri result = ContentUris.withAppendedId(uri, id);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (URI_MATCHER.match(uri) != POIS) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        int count = databaseHelper.getWritableDatabase().delete(LocalDatabaseHelper.TABLE_POI_CACHE, selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (URI_MATCHER.match(uri) != POIS) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        }
        int count = databaseHelper.getWritableDatabase().update(LocalDatabaseHelper.TABLE_POI_CACHE, values, selection, selectionArgs);
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
