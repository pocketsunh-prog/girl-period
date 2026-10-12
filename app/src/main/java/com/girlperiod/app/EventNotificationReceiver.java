package com.girlperiod.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * BroadcastReceiver that triggers the event notification service.
 * Runs daily via AlarmManager and reschedules on device boot.
 */
public class EventNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            // Reschedule the alarm on device boot
            rescheduleAlarm(context);
        }

        // Respect the notification switch in Settings
        if (!PeriodNotificationService.isNotificationsEnabled(context)) {
            return;
        }

        // Start the notification service
        EventNotificationService.createNotificationChannel(context);
        Intent serviceIntent = new Intent(context, EventNotificationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private void rescheduleAlarm(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        if (notificationsEnabled) {
            Intent alarmIntent = new Intent(context, EventNotificationReceiver.class);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, 1, alarmIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 8);
                calendar.set(java.util.Calendar.MINUTE, 0);
                calendar.set(java.util.Calendar.SECOND, 0);
                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
                }
                alarmManager.setInexactRepeating(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        android.app.AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        }
    }
}
