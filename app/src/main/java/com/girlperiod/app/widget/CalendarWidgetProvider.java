package com.girlperiod.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.girlperiod.app.AddPeriodActivity;
import com.girlperiod.app.MainActivity;
import com.girlperiod.app.R;
import com.girlperiod.app.SessionManager;
import com.girlperiod.app.data.DatabaseHelper;
import com.girlperiod.app.data.Event;
import com.girlperiod.app.data.PeriodRecord;
import com.girlperiod.app.data.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home-screen calendar widget.
 * <p>
 * Shows the current month in a compact dark card: weekend numbers in lime, today circled
 * in yellow, a lime dot for days that have events and a pink dot for period days. The
 * {@code 新增} button opens {@link AddPeriodActivity}; the month label and the day cells
 * open the app.
 */
public class CalendarWidgetProvider extends AppWidgetProvider {

    /** Refreshes every widget; also used as a manual refresh broadcast. */
    public static final String ACTION_REFRESH = "com.girlperiod.app.widget.ACTION_REFRESH";

    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_WEEKEND = 0xFFC8E64B;
    private static final int REQUEST_OPEN_APP = 10;
    private static final int REQUEST_ADD_PERIOD = 11;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            updateAllWidgets(context);
        }
    }

    /** Re-renders every placed widget; call after period/event data changes. */
    public static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, CalendarWidgetProvider.class));
        if (ids == null || ids.length == 0) {
            return;
        }
        for (int id : ids) {
            manager.updateAppWidget(id, buildRemoteViews(context));
        }
    }

    /**
     * Builds the widget for the current month. Public so the debug preview screen can
     * render exactly the same view hierarchy.
     */
    public static RemoteViews buildRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);

        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 0);
        month.set(Calendar.MINUTE, 0);
        month.set(Calendar.SECOND, 0);
        month.set(Calendar.MILLISECOND, 0);

        views.setTextViewText(R.id.widgetMonth,
                new SimpleDateFormat("MMM", Locale.ENGLISH).format(month.getTime()));

        Set<Integer> eventDays = loadEventDays(context, month);
        Set<Integer> periodDays = loadPeriodDays(context, month);

        int firstDayIndex = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0 = Sunday
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int weekRows = (firstDayIndex + daysInMonth + 6) / 7;
        Calendar today = Calendar.getInstance();

        // Hide week rows the month does not need so the grid fills the card evenly.
        for (int w = 0; w < WidgetIds.WEEK.length; w++) {
            views.setViewVisibility(WidgetIds.WEEK[w], w < weekRows ? View.VISIBLE : View.GONE);
        }

        boolean sameMonth = today.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                && today.get(Calendar.MONTH) == month.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < WidgetIds.CELL_COUNT; i++) {
            int day = i - firstDayIndex + 1;
            int dayView = WidgetIds.DAY[i];
            int todayView = WidgetIds.TODAY[i];

            if (day < 1 || day > daysInMonth) {
                views.setTextViewText(dayView, "");
                views.setViewVisibility(dayView, View.GONE);
                views.setViewVisibility(todayView, View.GONE);
                views.setViewVisibility(WidgetIds.DOT_EVENT[i], View.GONE);
                views.setViewVisibility(WidgetIds.DOT_PERIOD[i], View.GONE);
                continue;
            }

            String label = String.valueOf(day);
            views.setTextViewText(dayView, label);

            boolean weekend = (i % 7 == 0) || (i % 7 == 6);
            boolean isToday = sameMonth && day == todayDay;

            if (isToday) {
                // RemoteViews cannot swap drawables at runtime, so today uses a second
                // TextView with a yellow circle instead of changing the background.
                views.setTextViewText(todayView, label);
                views.setViewVisibility(todayView, View.VISIBLE);
                views.setViewVisibility(dayView, View.INVISIBLE);
            } else {
                views.setViewVisibility(todayView, View.GONE);
                views.setViewVisibility(dayView, View.VISIBLE);
                views.setTextColor(dayView, weekend ? COLOR_WEEKEND : COLOR_TEXT);
            }

            views.setViewVisibility(WidgetIds.DOT_EVENT[i],
                    eventDays.contains(day) ? View.VISIBLE : View.GONE);
            views.setViewVisibility(WidgetIds.DOT_PERIOD[i],
                    periodDays.contains(day) ? View.VISIBLE : View.GONE);
        }

        Intent openIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context, REQUEST_OPEN_APP, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widgetMonth, openPendingIntent);
        views.setOnClickPendingIntent(R.id.widgetTitle, openPendingIntent);
        for (int id : WidgetIds.DAY) {
            views.setOnClickPendingIntent(id, openPendingIntent);
        }

        Intent addIntent = new Intent(context, AddPeriodActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("selected_date", System.currentTimeMillis());
        PendingIntent addPendingIntent = PendingIntent.getActivity(
                context, REQUEST_ADD_PERIOD, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetAdd, addPendingIntent);

        return views;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /** Day-of-month numbers of events in the displayed month. */
    private static Set<Integer> loadEventDays(Context context, Calendar month) {
        Set<Integer> days = new HashSet<>();
        long userId = currentUserId(context);
        if (userId <= 0) {
            return days;
        }
        try {
            DatabaseHelper db = new DatabaseHelper(context);
            List<Event> events = db.getEventsByUser(userId);
            if (events == null) {
                return days;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            for (Event event : events) {
                if (event.getEventDate() == null) {
                    continue;
                }
                try {
                    Date date = format.parse(event.getEventDate());
                    if (date == null) {
                        continue;
                    }
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    if (cal.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                            && cal.get(Calendar.MONTH) == month.get(Calendar.MONTH)) {
                        days.add(cal.get(Calendar.DAY_OF_MONTH));
                    }
                } catch (Exception ignored) {
                    // Skip malformed dates
                }
            }
        } catch (Exception ignored) {
            // Widget must never crash; render without dots
        }
        return days;
    }

    /** Day-of-month numbers covered by recorded periods in the displayed month. */
    private static Set<Integer> loadPeriodDays(Context context, Calendar month) {
        Set<Integer> days = new HashSet<>();
        long userId = currentUserId(context);
        if (userId <= 0) {
            return days;
        }
        try {
            DatabaseHelper db = new DatabaseHelper(context);
            List<PeriodRecord> records = db.getPeriodRecordsByUser(userId);
            if (records == null) {
                return days;
            }

            Calendar monthStart = (Calendar) month.clone();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            Calendar monthEnd = (Calendar) month.clone();
            monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));

            for (PeriodRecord record : records) {
                Date start = record.getStartDateDate();
                if (start == null) {
                    continue;
                }
                Date end = record.getEndDateDate();
                if (end == null) {
                    end = start;
                }

                Calendar cursor = Calendar.getInstance();
                cursor.setTime(start);
                normalize(cursor);
                Calendar last = Calendar.getInstance();
                last.setTime(end);
                normalize(last);

                if (last.before(monthStart) || cursor.after(monthEnd)) {
                    continue;
                }
                if (cursor.before(monthStart)) {
                    cursor = (Calendar) monthStart.clone();
                }
                if (last.after(monthEnd)) {
                    last = (Calendar) monthEnd.clone();
                }

                // Guard against absurd ranges
                for (int i = 0; i < 60 && !cursor.after(last); i++) {
                    if (cursor.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                            && cursor.get(Calendar.MONTH) == month.get(Calendar.MONTH)) {
                        days.add(cursor.get(Calendar.DAY_OF_MONTH));
                    }
                    cursor.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        } catch (Exception ignored) {
            // Widget must never crash; render without dots
        }
        return days;
    }

    /** Id of the logged-in user, falling back to a username lookup. */
    private static long currentUserId(Context context) {
        try {
            SessionManager session = new SessionManager(context);
            if (!session.isLoggedIn()) {
                return -1;
            }
            User user = session.getCurrentUser();
            if (user == null) {
                return -1;
            }
            long id = user.getId();
            if (id <= 0 && user.getUsername() != null) {
                DatabaseHelper db = new DatabaseHelper(context);
                User stored = db.getUserByUsername(user.getUsername());
                if (stored != null) {
                    id = stored.getId();
                }
            }
            return id;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void normalize(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
