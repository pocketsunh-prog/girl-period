package com.girlperiod.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Background (foreground) service that watches the predicted period date and
 * reminds the user when the period is coming within {@link #REMIND_DAYS_BEFORE} days.
 * <p>
 * It repeats its check every hour while running, survives being killed
 * ({@code START_STICKY}) and is re-armed daily by {@link NotificationReceiver} and on
 * device boot. Each countdown stage (2 days / 1 day / today) notifies at most once per day.
 */
public class PeriodNotificationService extends Service {

    /** Shared settings file name, shared with {@link SettingsActivity}. */
    public static final String PREFS_SETTINGS = "app_settings";
    public static final String KEY_NOTIFICATIONS = "notifications_enabled";

    private static final String CHANNEL_ID = "period_reminder_channel";
    private static final String CHANNEL_NAME = "Period Reminders";
    private static final int FOREGROUND_NOTIFICATION_ID = 1000;
    private static final int BASE_NOTIFICATION_ID = 1001;
    private static final long REMINDER_INTERVAL_MS = 60 * 60 * 1000; // 1 hour
    private static final String KEY_LAST_NOTIFIED_PREFIX = "period_notified_stage_";

    /** How many days ahead of the predicted period the user is warned. */
    public static final int REMIND_DAYS_BEFORE = 2;

    private static final String[] GHIBLI_MESSAGES = {
        "The wind is changing… your cycle is near.",
        "A gentle whisper from the forest spirits — your period arrives soon.",
        "Like cherry blossoms in spring, nature has its own rhythm.",
        "The river flows, the moon turns, and so does your cycle.",
        "Even Totoro knows — it's time to prepare!",
        "A little kindness to yourself goes a long way. Your period is near.",
        "The seasons turn softly… and so does your body.",
        "A warm cup of tea and a cozy blanket await you."
    };

    private Handler handler;
    private Runnable checkRunnable;

    // ------------------------------------------------------------------
    // Static helpers used by receivers, activities and settings
    // ------------------------------------------------------------------

    /** Starts the background reminder service (foreground service on O+). */
    public static void start(Context context) {
        createNotificationChannel(context);
        Intent serviceIntent = new Intent(context, PeriodNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /** Stops the background reminder service. */
    public static void stop(Context context) {
        context.stopService(new Intent(context, PeriodNotificationService.class));
    }

    /** True when the user has the notification switch turned on in Settings. */
    public static boolean isNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATIONS, true);
    }

    /** Arms the daily 8:00 AM check (also re-armed on boot). */
    public static void scheduleDaily(Context context) {
        createNotificationChannel(context);
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    /** Cancels the daily check. */
    public static void cancelDaily(Context context) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    // ------------------------------------------------------------------
    // Service lifecycle
    // ------------------------------------------------------------------

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Must call startForeground() promptly: the receiver starts us with
        // startForegroundService(), and Android kills the process otherwise.
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        checkAndNotify();

        // Only ever keep one loop alive, however often onStartCommand is called.
        if (checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkAndNotify();
                handler.postDelayed(this, REMINDER_INTERVAL_MS);
            }
        };
        handler.postDelayed(checkRunnable, REMINDER_INTERVAL_MS);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && checkRunnable != null) {
            handler.removeCallbacks(checkRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Low-priority ongoing notification that keeps the service alive. */
    private Notification createForegroundNotification() {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Period Reminder")
                .setContentText("Watching your cycle quietly in the background…")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    // ------------------------------------------------------------------
    // Reminder logic
    // ------------------------------------------------------------------

    /**
     * Queries the database for period entries, predicts the next period date and
     * shows a reminder when it is expected within {@link #REMIND_DAYS_BEFORE} days.
     */
    public void checkAndNotify() {
        Context context = getApplicationContext();

        if (!isNotificationsEnabled(context)) {
            return;
        }

        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) {
            return;
        }

        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        long userId = currentUser.getId();
        if (userId <= 0 && currentUser.getUsername() != null) {
            User stored = dbHelper.getUserByUsername(currentUser.getUsername());
            if (stored != null) {
                userId = stored.getId();
            }
        }

        List<PeriodRecord> records = dbHelper.getPeriodRecordsByUser(userId);
        if (records == null || records.isEmpty()) {
            return;
        }

        int daysUntil = PeriodPredictor.getDaysUntilNextPeriod(records);
        if (daysUntil < 0 || daysUntil > REMIND_DAYS_BEFORE) {
            return;
        }

        // Notify once per day for each countdown stage so the hourly check
        // does not spam the notification shade.
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String stageKey = KEY_LAST_NOTIFIED_PREFIX + daysUntil;
        if (today.equals(prefs.getString(stageKey, ""))) {
            return;
        }
        prefs.edit().putString(stageKey, today).apply();

        showNotification(context, daysUntil);
    }

    /** Builds and displays the period reminder notification. */
    private void showNotification(Context context, int daysUntil) {
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String ghibliMessage = GHIBLI_MESSAGES[(int) (System.currentTimeMillis() % GHIBLI_MESSAGES.length)];

        String title;
        if (daysUntil == 0) {
            title = "Your period may start today!";
        } else if (daysUntil == 1) {
            title = "Your period is expected tomorrow!";
        } else {
            title = "Your period is expected in " + daysUntil + " days!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title)
                .setContentText(ghibliMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(ghibliMessage))
                .setPriority(daysUntil == 0
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (daysUntil == 0) {
            builder.setVibrate(new long[]{0, 400, 200, 400});
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(BASE_NOTIFICATION_ID + daysUntil, builder.build());
        }
    }

    /** Creates the notification channel required for Android O and above. */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Gentle reminders about your upcoming period");
            channel.enableVibration(true);

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
