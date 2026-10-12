package com.girlperiod.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver that triggers the daily period-check.
 * <p>
 * AlarmManager fires it once per day (08:00) and it is also registered for
 * {@code BOOT_COMPLETED}; either way it re-arms the alarm and starts
 * {@link PeriodNotificationService}, which performs the database query and shows
 * a reminder when the period is expected within 2 days.
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            // Alarms do not survive a reboot — re-arm the daily check.
            if (PeriodNotificationService.isNotificationsEnabled(context)) {
                PeriodNotificationService.scheduleDaily(context);
            }
        }

        if (!PeriodNotificationService.isNotificationsEnabled(context)) {
            return;
        }

        PeriodNotificationService.start(context);
    }
}
