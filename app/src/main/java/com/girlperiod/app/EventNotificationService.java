package com.girlperiod.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

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
 * Service that checks for upcoming events and shows notifications.
 */
public class EventNotificationService extends Service {

    private static final String CHANNEL_ID = "event_reminder_channel";
    private static final String CHANNEL_NAME = "Event Reminders";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkAndNotify();
        stopSelf();
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Queries the database for events and shows notifications for upcoming events.
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
                    showNotification(context, event, (int) diffDays);
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }
    }

    /**
     * Shows a notification for an upcoming event.
     */
    private void showNotification(Context context, Event event, int daysUntil) {
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String message;
        if (daysUntil == 0) {
            message = event.getTitle() + " is today!";
        } else if (daysUntil == 1) {
            message = event.getTitle() + " is tomorrow!";
        } else {
            message = event.getTitle() + " is in " + daysUntil + " days!";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Upcoming Event")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) event.getId(), builder.build());
        }
    }

    /**
     * Creates the notification channel required for Android O and above.
     */
    public static void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Reminders for upcoming events");

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
