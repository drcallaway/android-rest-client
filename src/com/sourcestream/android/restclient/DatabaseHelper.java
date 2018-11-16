package com.sourcestream.android.restclient;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Database helper.
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "rest_client.db";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE " + RequestTable.TABLE_NAME + " ("
            + RequestTable._ID + " INTEGER PRIMARY KEY,"
            + RequestTable.COLUMN_NAME_NAME + " TEXT,"
            + RequestTable.COLUMN_NAME_METHOD + " TEXT,"
            + RequestTable.COLUMN_NAME_URL + " TEXT,"
            + RequestTable.COLUMN_NAME_BODY + " TEXT"
            + ");");

        db.execSQL("CREATE TABLE " + HeaderTable.TABLE_NAME + " ("
            + HeaderTable._ID + " INTEGER PRIMARY KEY,"
            + HeaderTable.COLUMN_NAME_NAME + " TEXT,"
            + HeaderTable.COLUMN_NAME_VALUE + " TEXT,"
            + HeaderTable.COLUMN_NAME_REQUEST_ID + " INTEGER,"
            + "FOREIGN KEY(" + HeaderTable.COLUMN_NAME_REQUEST_ID + ") REFERENCES " + RequestTable.TABLE_NAME
            + "(" + RequestTable._ID + ") ON DELETE CASCADE"
            + ");");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // Kills the table and existing data
        db.execSQL("DROP TABLE IF EXISTS " + HeaderTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RequestTable.TABLE_NAME);

        // Recreates the database with a new version
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db)
    {
        super.onOpen(db);
        if (!db.isReadOnly())
        {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    public static class RequestTable implements BaseColumns
    {
        public static final String TABLE_NAME = "request";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_METHOD = "method";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_BODY = "body";
    }

    public static class HeaderTable implements BaseColumns
    {
        public static final String TABLE_NAME = "header";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_VALUE = "value";
        public static final String COLUMN_NAME_REQUEST_ID = "request_id";
    }
}
