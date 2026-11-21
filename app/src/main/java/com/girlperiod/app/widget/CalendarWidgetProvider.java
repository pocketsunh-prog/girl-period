package com.girlperiod.app.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import com.girlperiod.app.AddPeriodActivity;
import com.girlperiod.app.FestivalHelper;
import com.girlperiod.app.LunarCalendar;
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
import java.util.Map;
import java.util.Set;

/**
 * Home-screen calendar widget styled like the in-app calendar.
 * <p>
 * Shows the month with prev/next arrows, English weekday labels, the Chinese lunar day
 * under each date, a weather emoji + temperature (from the app's last HKO fetch), orange
 * dots for events, a star for festivals, pink/purple rounded highlights for period days
 * and a green highlight plus the long-cat icon on today.
 * <p>
 * Today is recomputed on every render, so the pointer moves automatically: the widget is
 * refreshed at midnight by the system's {@code DATE_CHANGED} broadcast and by a daily
 * {@link AlarmManager} alarm, and immediately after app data changes.
 */
public class CalendarWidgetProvider extends AppWidgetProvider {

    /** Manual refresh broadcast (also used after weather or data changes). */
    public static final String ACTION_REFRESH = "com.girlperiod.app.widget.ACTION_REFRESH";

    private static final String ACTION_NAV = "com.girlperiod.app.widget.ACTION_NAV";
    private static final String ACTION_TODAY = "com.girlperiod.app.widget.ACTION_TODAY";
    private static final String ACTION_DAILY = "com.girlperiod.app.widget.ACTION_DAILY";
    private static final String EXTRA_WIDGET_ID = "widget_id";
    private static final String EXTRA_DELTA = "delta";

    private static final String PREFS = "widget_prefs";
    private static final String KEY_OFFSET = "month_offset_";
    private static final int MAX_MONTH_OFFSET = 24;

    private static final int DAILY_ALARM_REQUEST_CODE = 77;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        scheduleDailyRefresh(context);
        for (int id : appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (action == null) {
            return;
        }

        if (ACTION_NAV.equals(action)) {
            int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                int delta = intent.getIntExtra(EXTRA_DELTA, 0);
                SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                int offset = prefs.getInt(KEY_OFFSET + widgetId, 0) + delta;
                offset = Math.max(-MAX_MONTH_OFFSET, Math.min(MAX_MONTH_OFFSET, offset));
                prefs.edit().putInt(KEY_OFFSET + widgetId, offset).apply();
                updateWidget(context, widgetId);
            }
            return;
        }

        if (ACTION_TODAY.equals(action)) {
            int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_OFFSET + widgetId, 0)
                        .apply();
                updateWidget(context, widgetId);
            }
            return;
        }

        if (ACTION_DAILY.equals(action)
                || ACTION_REFRESH.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // Re-render so the "today" pointer follows the date.
            updateAllWidgets(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleDailyRefresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelDailyRefresh(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        for (int id : appWidgetIds) {
            editor.remove(KEY_OFFSET + id);
        }
        editor.apply();
    }

    /** Re-renders every placed widget; call after period/event/weather data changes. */
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
            manager.updateAppWidget(id, buildRemoteViews(context, id));
        }
        scheduleDailyRefresh(context);
    }

    private static void updateWidget(Context context, int widgetId) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager != null) {
            manager.updateAppWidget(widgetId, buildRemoteViews(context, widgetId));
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * Builds the widget for the month the user is browsing ({@code widgetId} keeps its
     * own month offset). Public so the debug preview screen can render the same views.
     */
    public static RemoteViews buildRemoteViews(Context context, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);

        Calendar today = Calendar.getInstance();
        Calendar month = (Calendar) today.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);
        normalize(month);

        int offset = 0;
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            offset = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getInt(KEY_OFFSET + widgetId, 0);
        }
        month.add(Calendar.MONTH, offset);

        int year = month.get(Calendar.YEAR);
        int monthIndex = month.get(Calendar.MONTH);
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayIndex = month.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; // 0 = Sunday
        int weekRows = (firstDayIndex + daysInMonth + 6) / 7;

        boolean sameMonth = offset == 0;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        views.setTextViewText(R.id.widgetMonth,
                new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.getTime()));

        for (int w = 0; w < WidgetIds.WEEK.length; w++) {
            views.setViewVisibility(WidgetIds.WEEK[w], w < weekRows ? View.VISIBLE : View.GONE);
        }

        Set<Integer> eventDays = loadEventDays(context, year, monthIndex);
        Set<Integer> startDays = new HashSet<>();
        Set<Integer> endDays = new HashSet<>();
        Set<Integer> middleDays = new HashSet<>();
        loadPeriodDays(context, year, monthIndex, daysInMonth, startDays, endDays, middleDays);
        Map<String, String> weather = WidgetWeatherCache.load(context);

        for (int i = 0; i < WidgetIds.CELL_COUNT; i++) {
            int day = i - firstDayIndex + 1;
            int bgView = WidgetIds.BG[i];
            int numView = WidgetIds.NUM[i];

            if (day < 1 || day > daysInMonth) {
                views.setViewVisibility(bgView, View.GONE);
                views.setTextViewText(numView, "");
                views.setTextViewText(WidgetIds.LUNAR[i], "");
                views.setTextViewText(WidgetIds.WEATHER[i], "");
                views.setViewVisibility(WidgetIds.CAT[i], View.GONE);
                views.setViewVisibility(WidgetIds.STAR[i], View.GONE);
                views.setViewVisibility(WidgetIds.DOT[i], View.GONE);
                continue;
            }

            views.setViewVisibility(bgView, View.VISIBLE);
            views.setTextViewText(numView, String.valueOf(day));

            boolean isToday = sameMonth && day == todayDay;

            // Lunar day (last two characters, e.g. 廿一)
            String lunar = LunarCalendar.getLunarDate(year, monthIndex + 1, day);
            String lunarShort = lunar != null && lunar.length() >= 2
                    ? lunar.substring(lunar.length() - 2) : "";

            views.setViewVisibility(WidgetIds.CAT[i], isToday ? View.VISIBLE : View.GONE);
            views.setTextViewText(WidgetIds.LUNAR[i], isToday ? "" : lunarShort);

            // Weather emoji + temperature for forecast days the app has cached
            String weatherText = weather.get(String.format(Locale.ENGLISH, "%04d-%02d-%02d",
                    year, monthIndex + 1, day));
            if (weatherText == null) {
                weatherText = "";
            }
            views.setTextViewText(WidgetIds.WEATHER[i], weatherText);

            // Cell highlight: today > period start > period end > period middle > normal
            int background;
            if (isToday) {
                background = R.drawable.widget_cell_today;
            } else if (startDays.contains(day)) {
                background = R.drawable.widget_cell_start;
            } else if (endDays.contains(day)) {
                background = R.drawable.widget_cell_end;
            } else if (middleDays.contains(day)) {
                background = R.drawable.widget_cell_period;
            } else {
                background = R.drawable.widget_cell_bg;
            }
            views.setImageViewResource(bgView, background);

            boolean festival = FestivalHelper.getFestival(year, monthIndex + 1, day) != null;
            views.setViewVisibility(WidgetIds.STAR[i], festival ? View.VISIBLE : View.GONE);
            views.setViewVisibility(WidgetIds.DOT[i],
                    eventDays.contains(day) ? View.VISIBLE : View.GONE);

            views.setOnClickPendingIntent(bgView,
                    addPeriodIntent(context, widgetId, i, month, day));
        }

        views.setOnClickPendingIntent(R.id.widgetPrev,
                navIntent(context, widgetId, -1, 1));
        views.setOnClickPendingIntent(R.id.widgetNext,
                navIntent(context, widgetId, 1, 2));
        views.setOnClickPendingIntent(R.id.widgetMonth,
                todayIntent(context, widgetId));

        return views;
    }

    // ------------------------------------------------------------------
    // Pending intents
    // ------------------------------------------------------------------

    private static PendingIntent addPeriodIntent(Context context, int widgetId, int cellIndex,
                                                 Calendar month, int day) {
        Calendar date = (Calendar) month.clone();
        date.set(Calendar.DAY_OF_MONTH, day);
        Intent intent = new Intent(context, AddPeriodActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("selected_date", date.getTimeInMillis());
        int requestCode = (widgetId + 1) * 100 + cellIndex;
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent navIntent(Context context, int widgetId, int delta, int slot) {
        Intent intent = new Intent(context, CalendarWidgetProvider.class)
                .setAction(ACTION_NAV)
                .putExtra(EXTRA_WIDGET_ID, widgetId)
                .putExtra(EXTRA_DELTA, delta);
        int requestCode = (widgetId + 1) * 10 + slot;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent todayIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, CalendarWidgetProvider.class)
                .setAction(ACTION_TODAY)
                .putExtra(EXTRA_WIDGET_ID, widgetId);
        int requestCode = (widgetId + 1) * 10 + 3;
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ------------------------------------------------------------------
    // Daily refresh
    // ------------------------------------------------------------------

    /** Schedules a daily midnight re-render so the today pointer never gets stale. */
    public static void scheduleDailyRefresh(Context context) {
        try {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }
            Intent intent = new Intent(context, CalendarWidgetProvider.class)
                    .setAction(ACTION_DAILY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, DAILY_ALARM_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar next = Calendar.getInstance();
            next.add(Calendar.DAY_OF_MONTH, 1);
            next.set(Calendar.HOUR_OF_DAY, 0);
            next.set(Calendar.MINUTE, 1);
            next.set(Calendar.SECOND, 0);
            next.set(Calendar.MILLISECOND, 0);

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    next.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
        } catch (Exception ignored) {
            // Falls back to the system's DATE_CHANGED broadcast
        }
    }

    static void cancelDailyRefresh(Context context) {
        try {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }
            Intent intent = new Intent(context, CalendarWidgetProvider.class)
                    .setAction(ACTION_DAILY);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, DAILY_ALARM_REQUEST_CODE, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(pendingIntent);
        } catch (Exception ignored) {
            // Nothing to cancel
        }
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    /** Day-of-month numbers of events in the displayed month. */
    private static Set<Integer> loadEventDays(Context context, int year, int monthIndex) {
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
                    if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == monthIndex) {
                        days.add(cal.get(Calendar.DAY_OF_MONTH));
                    }
                } catch (Exception ignored) {
                    // Skip malformed dates
                }
            }
        } catch (Exception ignored) {
            // The widget must never crash; render without events
        }
        return days;
    }

    /** Fills the start / end / middle day sets of period records for the displayed month. */
    private static void loadPeriodDays(Context context, int year, int monthIndex, int daysInMonth,
                                       Set<Integer> startDays, Set<Integer> endDays,
                                       Set<Integer> middleDays) {
        long userId = currentUserId(context);
        if (userId <= 0) {
            return;
        }
        try {
            DatabaseHelper db = new DatabaseHelper(context);
            List<PeriodRecord> records = db.getPeriodRecordsByUser(userId);
            if (records == null) {
                return;
            }

            Calendar monthStart = Calendar.getInstance();
            monthStart.clear();
            monthStart.set(year, monthIndex, 1);

            Calendar monthEnd = Calendar.getInstance();
            monthEnd.clear();
            monthEnd.set(year, monthIndex, daysInMonth);

            for (PeriodRecord record : records) {
                Date start = record.getStartDateDate();
                if (start == null) {
                    continue;
                }
                Date end = record.getEndDateDate();
                if (end == null) {
                    end = start;
                }

                Calendar startCal = Calendar.getInstance();
                startCal.setTime(start);
                normalize(startCal);
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(end);
                normalize(endCal);

                if (endCal.before(monthStart) || startCal.after(monthEnd)) {
                    continue;
                }

                Calendar cursor = startCal.before(monthStart)
                        ? (Calendar) monthStart.clone() : (Calendar) startCal.clone();
                Calendar last = endCal.after(monthEnd)
                        ? (Calendar) monthEnd.clone() : (Calendar) endCal.clone();

                for (int guard = 0; guard < 60 && !cursor.after(last); guard++) {
                    int day = cursor.get(Calendar.DAY_OF_MONTH);
                    if (cursor.get(Calendar.YEAR) == year
                            && cursor.get(Calendar.MONTH) == monthIndex) {
                        if (sameDay(cursor, startCal)) {
                            startDays.add(day);
                        } else if (sameDay(cursor, endCal)) {
                            endDays.add(day);
                        } else {
                            middleDays.add(day);
                        }
                    }
                    cursor.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        } catch (Exception ignored) {
            // The widget must never crash; render without period marks
        }
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

    private static boolean sameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.MONTH) == second.get(Calendar.MONTH)
                && first.get(Calendar.DAY_OF_MONTH) == second.get(Calendar.DAY_OF_MONTH);
    }

    private static void normalize(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
