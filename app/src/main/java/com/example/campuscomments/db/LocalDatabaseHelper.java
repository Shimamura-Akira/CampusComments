package com.example.campuscomments.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.campuscomments.model.CampusPoi;

import java.util.List;

public class LocalDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "campus_comments.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_POI_CACHE = "poi_cache";
    public static final String COL_OBJECT_ID = "object_id";
    public static final String COL_NAME = "name";
    public static final String COL_TYPE = "type";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_ADDRESS = "address";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_AVG_SCORE = "avg_score";
    public static final String COL_REVIEW_COUNT = "review_count";
    public static final String COL_UPDATED_AT = "updated_at";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_POI_CACHE + " ("
                + COL_OBJECT_ID + " TEXT PRIMARY KEY, "
                + COL_NAME + " TEXT, "
                + COL_TYPE + " TEXT, "
                + COL_LATITUDE + " REAL, "
                + COL_LONGITUDE + " REAL, "
                + COL_ADDRESS + " TEXT, "
                + COL_DESCRIPTION + " TEXT, "
                + COL_AVG_SCORE + " REAL, "
                + COL_REVIEW_COUNT + " INTEGER, "
                + COL_UPDATED_AT + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POI_CACHE);
        onCreate(db);
    }

    public void replacePoiCache(List<CampusPoi> pois) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_POI_CACHE, null, null);
            for (CampusPoi poi : pois) {
                ContentValues values = new ContentValues();
                values.put(COL_OBJECT_ID, poi.getObjectId());
                values.put(COL_NAME, poi.getName());
                values.put(COL_TYPE, poi.getType());
                values.put(COL_LATITUDE, poi.getLatitude());
                values.put(COL_LONGITUDE, poi.getLongitude());
                values.put(COL_ADDRESS, poi.getAddress());
                values.put(COL_DESCRIPTION, poi.getDescription());
                values.put(COL_AVG_SCORE, poi.getAvgScore());
                values.put(COL_REVIEW_COUNT, poi.getReviewCount());
                values.put(COL_UPDATED_AT, poi.getUpdatedAt());
                db.insertWithOnConflict(TABLE_POI_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
