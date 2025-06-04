package com.example.snapnews.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class NewsDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "NewsDatabaseHelper";

    // Database Info
    private static final String DATABASE_NAME = "news_database.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_ARTICLES = "articles";

    // Article Table Columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_URL_TO_IMAGE = "urlToImage";
    public static final String COLUMN_PUBLISHED_AT = "publishedAt";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_SOURCE_ID = "sourceId";
    public static final String COLUMN_SOURCE_NAME = "sourceName";
    public static final String COLUMN_IS_FAVORITE = "isFavorite";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Create Articles Table SQL
    private static final String CREATE_ARTICLES_TABLE =
            "CREATE TABLE " + TABLE_ARTICLES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_URL + " TEXT UNIQUE, " +
                    COLUMN_URL_TO_IMAGE + " TEXT, " +
                    COLUMN_PUBLISHED_AT + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_AUTHOR + " TEXT, " +
                    COLUMN_SOURCE_ID + " TEXT, " +
                    COLUMN_SOURCE_NAME + " TEXT, " +
                    COLUMN_IS_FAVORITE + " INTEGER DEFAULT 0, " +
                    COLUMN_TIMESTAMP + " INTEGER DEFAULT 0" +
                    ")";

    // Singleton instance
    private static NewsDatabaseHelper sInstance;

    // Private constructor untuk singleton
    private NewsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "NewsDatabaseHelper constructor called");
    }

    public static synchronized NewsDatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NewsDatabaseHelper(context.getApplicationContext());
            Log.d(TAG, "NewsDatabaseHelper instance created");
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables");
        try {
            db.execSQL(CREATE_ARTICLES_TABLE);
            Log.d(TAG, "Articles table created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion != newVersion) {
            try {
                // Drop existing tables
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_ARTICLES);
                Log.d(TAG, "Old tables dropped");

                // Recreate tables
                onCreate(db);
                Log.d(TAG, "Tables recreated successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error upgrading database", e);
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        Log.d(TAG, "Database opened");

        if (!db.isReadOnly()) {
            try {
                // Enable foreign key constraints
                db.execSQL("PRAGMA foreign_keys=ON;");
                Log.d(TAG, "Foreign keys enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error enabling foreign keys", e);
            }
        }
    }
}