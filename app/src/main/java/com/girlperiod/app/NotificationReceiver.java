package com.girlperiod.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * BroadcastReceiver that triggers the daily period-check.
 * <p>
 * Schedule this receiver with AlarmManager (e.g. setInexactRepeating or setExactAndAllowWhileIdle)
 * to run once per day. On receive it starts {@link PeriodNotificationService} which performs
 * the database query and shows a notification if needed.
 */
public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PeriodNotificationService.createNotificationChannel(context);

        Intent serviceIntent = new Intent(context, PeriodNotificationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
