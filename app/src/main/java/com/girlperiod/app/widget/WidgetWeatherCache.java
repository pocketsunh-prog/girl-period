package com.girlperiod.app.widget;

import android.content.Context;

import com.girlperiod.app.WeatherService;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores the last weather the app fetched (HKO) so the home-screen widget can show
 * a per-day icon and temperature without doing network I/O itself.
 * <p>
 * Written by {@code MainActivity} after a weather refresh, read by
 * {@link CalendarWidgetProvider}.
 */
public final class WidgetWeatherCache {

    private static final String PREFS = "widget_weather_cache";
    private static final String KEY_DAYS = "days";

    private WidgetWeatherCache() {
    }

    /** Saves {@code yyyy-MM-dd -> WeatherData} so the widget can render icons and temperatures. */
    public static void save(Context context, Map<String, WeatherService.WeatherData> data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, WeatherService.WeatherData> entry : data.entrySet()) {
                WeatherService.WeatherData value = entry.getValue();
                if (entry.getKey() == null || value == null) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("t", Math.round(value.getTemperature()));
                item.put("i", value.getIconCode() == null ? "" : value.getIconCode());
                json.put(entry.getKey(), item);
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_DAYS, json.toString())
                    .apply();
        } catch (Exception ignored) {
            // Weather is optional for the widget
        }
    }

    /** Returns {@code yyyy-MM-dd -> "☀ 28°"} for the cached forecast days. */
    public static Map<String, String> load(Context context) {
        Map<String, String> days = new HashMap<>();
        try {
            String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_DAYS, null);
            if (raw == null) {
                return days;
            }
            JSONObject json = new JSONObject(raw);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String date = keys.next();
                JSONObject item = json.optJSONObject(date);
                if (item == null) {
                    continue;
                }
                days.put(date, emojiFor(item.optString("i", "")) + " "
                        + item.optInt("t", 0) + "°");
            }
        } catch (Exception ignored) {
            // Fall through with an empty cache
        }
        return days;
    }

    /** Maps an HKO weather icon code to a small emoji for the widget cell. */
    private static String emojiFor(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }
        char first = code.charAt(0);
        switch (first) {
            case '5':
                if ("50".equals(code)) {
                    return "☀";
                }
                return "53".equals(code) || "54".equals(code) ? "☁" : "⛅";
            case '6':
            case '7':
                return "🌧";
            case '8':
                return "⛈";
            case '9':
                return "💨";
            default:
                return "";
        }
    }
}
