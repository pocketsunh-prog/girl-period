package com.girlperiod.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "girlperiod.db";
    private static final int DATABASE_VERSION = 1;

    // Table: users
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_FINGERPRINT_ENABLED = "fingerprint_enabled";
    private static final String COL_CREATED_AT = "created_at";

    // Table: period_records
    private static final String TABLE_PERIOD_RECORDS = "period_records";
    private static final String COL_RECORD_ID = "id";
    private static final String COL_RECORD_USER_ID = "user_id";
    private static final String COL_START_DATE = "start_date";
    private static final String COL_END_DATE = "end_date";
    private static final String COL_CYCLE_LENGTH = "cycle_length";
    private static final String COL_NOTES = "notes";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_FINGERPRINT_ENABLED + " INTEGER DEFAULT 0, "
                + COL_CREATED_AT + " TEXT"
                + ")";
        db.execSQL(createUsersTable);

        String createPeriodRecordsTable = "CREATE TABLE " + TABLE_PERIOD_RECORDS + " ("
                + COL_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_RECORD_USER_ID + " INTEGER, "
                + COL_START_DATE + " TEXT NOT NULL, "
                + COL_END_DATE + " TEXT, "
                + COL_CYCLE_LENGTH + " INTEGER, "
                + COL_NOTES + " TEXT, "
                + "FOREIGN KEY(" + COL_RECORD_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")"
                + ")";
        db.execSQL(createPeriodRecordsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PERIOD_RECORDS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // --- User methods ---

    public long createUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, user.getUsername());
        values.put(COL_PASSWORD, user.getPassword());
        values.put(COL_FINGERPRINT_ENABLED, user.isFingerprintEnabled() ? 1 : 0);
        values.put(COL_CREATED_AT, user.getCreatedAt());
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public long addUser(String username, String password, boolean enableFingerprint) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_FINGERPRINT_ENABLED, enableFingerprint ? 1 : 0);
        values.put(COL_CREATED_AT, String.valueOf(System.currentTimeMillis()));
        long id = db.insert(TABLE_USERS, null, values);
        db.close();
        return id;
    }

    public boolean addUserBool(String username, String password, boolean enableFingerprint) {
        return addUser(username, password, enableFingerprint) != -1;
    }

    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password}, null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return valid;
    }

    public User validateUserAndReturn(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password}, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        db.close();
        return user;
    }

    public boolean isFingerprintEnabled(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_FINGERPRINT_ENABLED},
                COL_USERNAME + " = ?", new String[]{username}, null, null, null);
        boolean enabled = false;
        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(0) == 1;
        }
        cursor.close();
        db.close();
        return enabled;
    }

    public boolean isUsernameTaken(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USERNAME + " = ?", new String[]{username}, null, null, null);
        boolean taken = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return taken;
    }

    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USERNAME + " = ?",
                new String[]{username}, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        db.close();
        return user;
    }

    public User getUserById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COL_USER_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        db.close();
        return user;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_USER_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)));
        user.setFingerprintEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(COL_FINGERPRINT_ENABLED)) == 1);
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        return user;
    }

    // --- Period record methods ---

    public long addPeriodRecord(PeriodRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_USER_ID, record.getUserId());
        values.put(COL_START_DATE, record.getStartDateString());
        values.put(COL_END_DATE, record.getEndDateString());
        values.put(COL_CYCLE_LENGTH, record.getCycleLength());
        values.put(COL_NOTES, record.getNotes());
        long id = db.insert(TABLE_PERIOD_RECORDS, null, values);
        db.close();
        return id;
    }

    public List<PeriodRecord> getPeriodRecordsByUser(long userId) {
        List<PeriodRecord> records = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PERIOD_RECORDS, null, COL_RECORD_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, COL_START_DATE + " DESC");
        if (cursor.moveToFirst()) {
            do {
                records.add(cursorToPeriodRecord(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return records;
    }

    public int updatePeriodRecord(PeriodRecord record) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_RECORD_USER_ID, record.getUserId());
        values.put(COL_START_DATE, record.getStartDateString());
        values.put(COL_END_DATE, record.getEndDateString());
        values.put(COL_CYCLE_LENGTH, record.getCycleLength());
        values.put(COL_NOTES, record.getNotes());
        int rows = db.update(TABLE_PERIOD_RECORDS, values, COL_RECORD_ID + " = ?",
                new String[]{String.valueOf(record.getId())});
        db.close();
        return rows;
    }

    public int deletePeriodRecord(long recordId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PERIOD_RECORDS, COL_RECORD_ID + " = ?",
                new String[]{String.valueOf(recordId)});
        db.close();
        return rows;
    }

    public PeriodRecord getLastPeriod(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PERIOD_RECORDS, null, COL_RECORD_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, COL_START_DATE + " DESC", "1");
        PeriodRecord record = null;
        if (cursor.moveToFirst()) {
            record = cursorToPeriodRecord(cursor);
        }
        cursor.close();
        db.close();
        return record;
    }

    public int getAverageCycleLength(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(" + COL_CYCLE_LENGTH + ") FROM " + TABLE_PERIOD_RECORDS
                + " WHERE " + COL_RECORD_USER_ID + " = ? AND " + COL_CYCLE_LENGTH + " > 0",
                new String[]{String.valueOf(userId)});
        int average = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            average = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return average;
    }

    private PeriodRecord cursorToPeriodRecord(Cursor cursor) {
        PeriodRecord record = new PeriodRecord();
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECORD_ID)));
        record.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_RECORD_USER_ID)));
        record.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_START_DATE)));
        record.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_END_DATE)));
        record.setCycleLength(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CYCLE_LENGTH)));
        record.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)));
        return record;
    }
}
