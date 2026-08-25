package com.girlperiod.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;

import java.util.List;

/**
 * Service that checks whether the user's period is expected within the next 2 days
 * and posts a gentle Ghibli-style reminder notification.
 */
public class PeriodNotificationService extends Service {

    private static final String CHANNEL_ID = "period_reminder_channel";
    private static final String CHANNEL_NAME = "Period Reminders";
    private static final int NOTIFICATION_ID = 1001;

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
     * Queries the database for period entries, predicts the next period date,
     * and shows a notification if the period is expected within 2 days.
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
        List<PeriodRecord> records = dbHelper.getPeriodRecordsByUser(currentUser.getId());

        if (records == null || records.isEmpty()) {
            return;
        }

        int daysUntil = PeriodPredictor.getDaysUntilNextPeriod(records);

        if (daysUntil >= 0 && daysUntil <= 2) {
            showNotification(context, daysUntil);
        }
    }

    /**
     * Builds and displays the period reminder notification.
     */
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
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
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
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Gentle reminders about your upcoming period");

            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
