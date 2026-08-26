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
    private static final int DATABASE_VERSION = 2;

    // Table: users
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_FINGERPRINT_ENABLED = "fingerprint_enabled";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_DOB = "dob";
    private static final String COL_PROFILE_IMAGE = "profile_image";
    private static final String COL_LATITUDE = "latitude";
    private static final String COL_LONGITUDE = "longitude";
    private static final String COL_CITY_NAME = "city_name";

    // Table: period_records
    private static final String TABLE_PERIOD_RECORDS = "period_records";
    private static final String COL_RECORD_ID = "id";
    private static final String COL_RECORD_USER_ID = "user_id";
    private static final String COL_START_DATE = "start_date";
    private static final String COL_END_DATE = "end_date";
    private static final String COL_CYCLE_LENGTH = "cycle_length";
    private static final String COL_NOTES = "notes";

    // Table: events
    private static final String TABLE_EVENTS = "events";
    private static final String COL_EVENT_ID = "id";
    private static final String COL_EVENT_USER_ID = "user_id";
    private static final String COL_EVENT_TITLE = "title";
    private static final String COL_EVENT_DATE = "event_date";
    private static final String COL_EVENT_NOTES = "notes";
    private static final String COL_EVENT_REMINDER_DAYS = "reminder_days";
    private static final String COL_EVENT_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        ensureLocationColumnsExist();
    }

    /**
     * Ensure location columns exist in the users table.
     * This is a safety check for existing databases.
     */
    private void ensureLocationColumnsExist() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            // Check if latitude column exists
            android.database.Cursor cursor = db.rawQuery("PRAGMA table_info(" + TABLE_USERS + ")", null);
            boolean hasLatitude = false;
            boolean hasLongitude = false;
            boolean hasCityName = false;
            if (cursor.moveToFirst()) {
                do {
                    String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    if (COL_LATITUDE.equals(columnName)) hasLatitude = true;
                    if (COL_LONGITUDE.equals(columnName)) hasLongitude = true;
                    if (COL_CITY_NAME.equals(columnName)) hasCityName = true;
                } while (cursor.moveToNext());
            }
            cursor.close();

            // Add missing columns
            if (!hasLatitude) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LATITUDE + " REAL DEFAULT 0");
            }
            if (!hasLongitude) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LONGITUDE + " REAL DEFAULT 0");
            }
            if (!hasCityName) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CITY_NAME + " TEXT");
            }
        } catch (Exception e) {
            // Ignore errors
        }
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USERNAME + " TEXT UNIQUE NOT NULL, "
                + COL_PASSWORD + " TEXT NOT NULL, "
                + COL_FINGERPRINT_ENABLED + " INTEGER DEFAULT 0, "
                + COL_DOB + " TEXT, "
                + COL_PROFILE_IMAGE + " TEXT, "
                + COL_LATITUDE + " REAL DEFAULT 0, "
                + COL_LONGITUDE + " REAL DEFAULT 0, "
                + COL_CITY_NAME + " TEXT, "
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

        String createEventsTable = "CREATE TABLE " + TABLE_EVENTS + " ("
                + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EVENT_USER_ID + " INTEGER, "
                + COL_EVENT_TITLE + " TEXT NOT NULL, "
                + COL_EVENT_DATE + " TEXT NOT NULL, "
                + COL_EVENT_NOTES + " TEXT, "
                + COL_EVENT_REMINDER_DAYS + " INTEGER DEFAULT 1, "
                + COL_EVENT_CREATED_AT + " TEXT, "
                + "FOREIGN KEY(" + COL_EVENT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")"
                + ")";
        db.execSQL(createEventsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade from version 1 to 2: add location columns and events table
        if (oldVersion < 2) {
            // Add location columns to users table if they don't exist
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LATITUDE + " REAL DEFAULT 0");
            } catch (Exception e) {
                // Column may already exist
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LONGITUDE + " REAL DEFAULT 0");
            } catch (Exception e) {
                // Column may already exist
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CITY_NAME + " TEXT");
            } catch (Exception e) {
                // Column may already exist
            }
            
            // Create events table
            String createEventsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_EVENTS + " ("
                    + COL_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_EVENT_USER_ID + " INTEGER, "
                    + COL_EVENT_TITLE + " TEXT NOT NULL, "
                    + COL_EVENT_DATE + " TEXT NOT NULL, "
                    + COL_EVENT_NOTES + " TEXT, "
                    + COL_EVENT_REMINDER_DAYS + " INTEGER DEFAULT 1, "
                    + COL_EVENT_CREATED_AT + " TEXT, "
                    + "FOREIGN KEY(" + COL_EVENT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + ")"
                    + ")";
            db.execSQL(createEventsTable);
        }
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
        int dobIndex = cursor.getColumnIndex(COL_DOB);
        if (dobIndex >= 0 && !cursor.isNull(dobIndex)) {
            user.setDob(cursor.getString(dobIndex));
        }
        int imgIndex = cursor.getColumnIndex(COL_PROFILE_IMAGE);
        if (imgIndex >= 0 && !cursor.isNull(imgIndex)) {
            user.setProfileImage(cursor.getString(imgIndex));
        }
        int latIndex = cursor.getColumnIndex(COL_LATITUDE);
        if (latIndex >= 0) {
            user.setLatitude(cursor.getDouble(latIndex));
        }
        int lngIndex = cursor.getColumnIndex(COL_LONGITUDE);
        if (lngIndex >= 0) {
            user.setLongitude(cursor.getDouble(lngIndex));
        }
        int cityIndex = cursor.getColumnIndex(COL_CITY_NAME);
        if (cityIndex >= 0 && !cursor.isNull(cityIndex)) {
            user.setCityName(cursor.getString(cityIndex));
        }
        return user;
    }

    // --- User profile methods ---

    public int updateUserPassword(long userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows;
    }

    public int updateUserDob(long userId, String dob) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DOB, dob);
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows;
    }

    public int updateUserProfileImage(long userId, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROFILE_IMAGE, imagePath);
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows;
    }

    public int updateUserLocation(long userId, double latitude, double longitude, String cityName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_LATITUDE, latitude);
        values.put(COL_LONGITUDE, longitude);
        values.put(COL_CITY_NAME, cityName);
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows;
    }

    // --- Event methods ---

    public long addEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_USER_ID, event.getUserId());
        values.put(COL_EVENT_TITLE, event.getTitle());
        values.put(COL_EVENT_DATE, event.getEventDate());
        values.put(COL_EVENT_NOTES, event.getNotes());
        values.put(COL_EVENT_REMINDER_DAYS, event.getReminderDays());
        values.put(COL_EVENT_CREATED_AT, String.valueOf(System.currentTimeMillis()));
        long id = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return id;
    }

    public List<Event> getEventsByUser(long userId) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS, null, COL_EVENT_USER_ID + " = ?",
                new String[]{String.valueOf(userId)}, null, null, COL_EVENT_DATE + " ASC");
        if (cursor.moveToFirst()) {
            do {
                events.add(cursorToEvent(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return events;
    }

    public List<Event> getEventsByDate(long userId, String date) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EVENTS, null,
                COL_EVENT_USER_ID + " = ? AND " + COL_EVENT_DATE + " = ?",
                new String[]{String.valueOf(userId), date}, null, null, COL_EVENT_DATE + " ASC");
        if (cursor.moveToFirst()) {
            do {
                events.add(cursorToEvent(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return events;
    }

    public int updateEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EVENT_TITLE, event.getTitle());
        values.put(COL_EVENT_DATE, event.getEventDate());
        values.put(COL_EVENT_NOTES, event.getNotes());
        values.put(COL_EVENT_REMINDER_DAYS, event.getReminderDays());
        int rows = db.update(TABLE_EVENTS, values, COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(event.getId())});
        db.close();
        return rows;
    }

    public int deleteEvent(long eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EVENTS, COL_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)});
        db.close();
        return rows;
    }

    private Event cursorToEvent(Cursor cursor) {
        Event event = new Event();
        event.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_ID)));
        event.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_EVENT_USER_ID)));
        event.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_TITLE)));
        event.setEventDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_EVENT_DATE)));
        int notesIndex = cursor.getColumnIndex(COL_EVENT_NOTES);
        if (notesIndex >= 0 && !cursor.isNull(notesIndex)) {
            event.setNotes(cursor.getString(notesIndex));
        }
        int reminderIndex = cursor.getColumnIndex(COL_EVENT_REMINDER_DAYS);
        if (reminderIndex >= 0) {
            event.setReminderDays(cursor.getInt(reminderIndex));
        }
        return event;
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
