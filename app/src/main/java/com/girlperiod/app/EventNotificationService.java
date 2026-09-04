package com.girlperiod.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.Event;
import com.girlperiod.app.data.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Foreground service that checks for upcoming events and shows notifications.
 * Runs in background and repeats reminders based on reminder days.
 */
public class EventNotificationService extends Service {

    private static final String CHANNEL_ID = "event_reminder_channel";
    private static final String CHANNEL_NAME = "Event Reminders";
    private static final int FOREGROUND_NOTIFICATION_ID = 2000;
    private static final int BASE_NOTIFICATION_ID = 2001;
    private static final long REMINDER_INTERVAL_MS = 60 * 60 * 1000; // 1 hour between reminders

    private Handler handler;
    private Runnable checkRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service (background running)
        startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification());

        // Check immediately
        checkAndNotify();

        // Schedule periodic checks every hour for repeat reminders
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

    /**
     * Creates foreground notification for background running.
     */
    private Notification createForegroundNotification() {
        Intent tapIntent = new Intent(this, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Event Reminder")
                .setContentText("Monitoring upcoming events...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    /**
     * Queries the database for events and shows notifications.
     * Repeats reminders based on reminder days (e.g., 3 days = 3 reminders).
     */
    public void checkAndNotify() {
        Context context = getApplicationContext();
        SessionManager session = new SessionManager(context);
        if (!session.isLoggedIn()) {
            return;
        }

        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Event> events = dbHelper.getEventsByUser(currentUser.getId());

        if (events == null || events.isEmpty()) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());

        for (Event event : events) {
            try {
                Date eventDate = sdf.parse(event.getEventDate());
                Date today = sdf.parse(todayStr);

                if (eventDate == null || today == null) continue;

                long diffMillis = eventDate.getTime() - today.getTime();
                long diffDays = diffMillis / (24 * 60 * 60 * 1000);

                // Show notification if event is within reminder days
                if (diffDays >= 0 && diffDays <= event.getReminderDays()) {
                    // Send repeat reminders: 3 days before = 3 reminders, 2 days = 2 reminders, etc.
                    int repeatCount = (int) diffDays;
                    if (repeatCount == 0) repeatCount = 1; // At least 1 reminder for today
                    
                    for (int i = 0; i < repeatCount; i++) {
                        showNotification(context, event, (int) diffDays, i);
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
    }

    /**
     * Shows a notification for an upcoming event.
     * @param repeatIndex The repeat index (0 = first reminder, 1 = second, etc.)
     */
    private void showNotification(Context context, Event event, int daysUntil, int repeatIndex) {
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        tapIntent.putExtra("highlight_event_id", event.getId());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) event.getId() * 100 + repeatIndex,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title;
        String message;
        if (daysUntil == 0) {
            title = "Event Today!";
            message = event.getTitle() + " is today!";
        } else if (daysUntil == 1) {
            title = "Event Tomorrow!";
            message = event.getTitle() + " is tomorrow!";
        } else {
            title = "Upcoming Event (" + daysUntil + " days)";
            message = event.getTitle() + " is in " + daysUntil + " days!";
        }

        // Add repeat indicator
        if (repeatIndex > 0) {
            message += " (Reminder " + (repeatIndex + 1) + ")";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(daysUntil == 0 ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setNumber(repeatIndex + 1);

        // For today's event, use high priority with vibration
        if (daysUntil == 0) {
            builder.setVibrate(new long[]{0, 500, 200, 500});
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(BASE_NOTIFICATION_ID + (int) event.getId() * 100 + repeatIndex, builder.build());
        }
    }

    /**
     * Creates the notification channel required for Android O and above.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for upcoming events");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
