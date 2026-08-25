package com.girlperiod.app.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import com.girlperiod.app.R;

/**
 * Theme manager for the Ghibli cube-girl UI.
 * <p>
 * Provides five hand-picked color palettes inspired by Studio Ghibli aesthetics,
 * along with helpers to persist the user's choice and apply it to activities.
 */
public final class GhibliTheme {

    private static final String PREFS_NAME = "ghibli_theme_prefs";
    private static final String KEY_THEME = "selected_theme";
    private static final String KEY_CAL_TEXT_COLOR = "cal_text_color";
    private static final String KEY_CAL_BG_COLOR = "cal_bg_color";
    private static final String KEY_CAL_STYLE = "cal_style";
    private static final String KEY_DATE_PICKER_STYLE = "date_picker_style";

    private static final int DEFAULT_CAL_TEXT_COLOR = Color.parseColor("#000000");
    private static final int DEFAULT_CAL_BG_COLOR = Color.parseColor("#FFFFFF");
    public static final int CAL_STYLE_DEFAULT = 0;
    public static final int CAL_STYLE_COMPACT = 1;
    public static final int CAL_STYLE_ROUNDED = 2;
    public static final int CAL_STYLE_MINIMAL = 3;

    public static final int DATE_PICKER_STYLE_DEFAULT = 0;
    public static final int DATE_PICKER_STYLE_PINK = 1;
    public static final int DATE_PICKER_STYLE_GREEN = 2;
    public static final int DATE_PICKER_STYLE_BLUE = 3;
    public static final int DATE_PICKER_STYLE_PURPLE = 4;

    /* ------------------------------------------------------------------ */
    /*  Theme enum                                                        */
    /* ------------------------------------------------------------------ */

    public enum Theme {
        SAKURA,
        MATCHA,
        SKY,
        LAVENDER,
        PEACH
    }

    /* ------------------------------------------------------------------ */
    /*  Color palettes                                                    */
    /* ------------------------------------------------------------------ */

    // SAKURA — soft pink
    private static final int SAKURA_PRIMARY   = Color.parseColor("#F48FB1");
    private static final int SAKURA_ACCENT    = Color.parseColor("#FCE4EC");
    private static final int SAKURA_BACKGROUND = Color.parseColor("#FFF0F5");
    private static final int SAKURA_CARD      = Color.parseColor("#FFFFFF");
    private static final int SAKURA_TEXT      = Color.parseColor("#4A2040");

    // MATCHA — gentle green
    private static final int MATCHA_PRIMARY   = Color.parseColor("#81C784");
    private static final int MATCHA_ACCENT    = Color.parseColor("#E8F5E9");
    private static final int MATCHA_BACKGROUND = Color.parseColor("#F1F8E9");
    private static final int MATCHA_CARD      = Color.parseColor("#FFFFFF");
    private static final int MATCHA_TEXT      = Color.parseColor("#1B5E20");

    // SKY — calm blue
    private static final int SKY_PRIMARY      = Color.parseColor("#64B5F6");
    private static final int SKY_ACCENT       = Color.parseColor("#E3F2FD");
    private static final int SKY_BACKGROUND   = Color.parseColor("#F0F7FF");
    private static final int SKY_CARD         = Color.parseColor("#FFFFFF");
    private static final int SKY_TEXT         = Color.parseColor("#0D47A1");

    // LAVENDER — dreamy purple
    private static final int LAVENDER_PRIMARY = Color.parseColor("#CE93D8");
    private static final int LAVENDER_ACCENT  = Color.parseColor("#F3E5F5");
    private static final int LAVENDER_BACKGROUND = Color.parseColor("#FAF0FF");
    private static final int LAVENDER_CARD    = Color.parseColor("#FFFFFF");
    private static final int LAVENDER_TEXT    = Color.parseColor("#4A148C");

    // PEACH — warm orange
    private static final int PEACH_PRIMARY    = Color.parseColor("#FFAB91");
    private static final int PEACH_ACCENT     = Color.parseColor("#FFF3E0");
    private static final int PEACH_BACKGROUND = Color.parseColor("#FFF8F0");
    private static final int PEACH_CARD       = Color.parseColor("#FFFFFF");
    private static final int PEACH_TEXT       = Color.parseColor("#BF360C");

    /* ------------------------------------------------------------------ */
    /*  Theme color accessors                                             */
    /* ------------------------------------------------------------------ */

    public static int getPrimaryColor() {
        return getPrimaryColor(loadTheme(null));
    }

    public static int getPrimaryColor(Theme theme) {
        switch (theme) {
            case SAKURA:   return SAKURA_PRIMARY;
            case MATCHA:   return MATCHA_PRIMARY;
            case SKY:      return SKY_PRIMARY;
            case LAVENDER: return LAVENDER_PRIMARY;
            case PEACH:    return PEACH_PRIMARY;
            default:       return SAKURA_PRIMARY;
        }
    }

    public static int getAccentColor() {
        return getAccentColor(loadTheme(null));
    }

    public static int getAccentColor(Theme theme) {
        switch (theme) {
            case SAKURA:   return SAKURA_ACCENT;
            case MATCHA:   return MATCHA_ACCENT;
            case SKY:      return SKY_ACCENT;
            case LAVENDER: return LAVENDER_ACCENT;
            case PEACH:    return PEACH_ACCENT;
            default:       return SAKURA_ACCENT;
        }
    }

    public static int getBackgroundColor() {
        return getBackgroundColor(loadTheme(null));
    }

    public static int getBackgroundColor(Theme theme) {
        switch (theme) {
            case SAKURA:   return SAKURA_BACKGROUND;
            case MATCHA:   return MATCHA_BACKGROUND;
            case SKY:      return SKY_BACKGROUND;
            case LAVENDER: return LAVENDER_BACKGROUND;
            case PEACH:    return PEACH_BACKGROUND;
            default:       return SAKURA_BACKGROUND;
        }
    }

    public static int getCardColor() {
        return getCardColor(loadTheme(null));
    }

    public static int getCardColor(Theme theme) {
        switch (theme) {
            case SAKURA:   return SAKURA_CARD;
            case MATCHA:   return MATCHA_CARD;
            case SKY:      return SKY_CARD;
            case LAVENDER: return LAVENDER_CARD;
            case PEACH:    return PEACH_CARD;
            default:       return SAKURA_CARD;
        }
    }

    public static int getTextColor() {
        return getTextColor(loadTheme(null));
    }

    public static int getTextColor(Theme theme) {
        switch (theme) {
            case SAKURA:   return SAKURA_TEXT;
            case MATCHA:   return MATCHA_TEXT;
            case SKY:      return SKY_TEXT;
            case LAVENDER: return LAVENDER_TEXT;
            case PEACH:    return PEACH_TEXT;
            default:       return SAKURA_TEXT;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Persistence                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Saves the chosen theme name to SharedPreferences.
     */
    public static void saveTheme(Context context, String themeName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeName).apply();
    }

    /**
     * Loads the saved theme, defaulting to SAKURA if none is stored.
     */
    public static Theme loadTheme(Context context) {
        if (context == null) {
            return Theme.SAKURA;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_THEME, Theme.SAKURA.name());
        try {
            return Theme.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Theme.SAKURA;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Apply theme to activity                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Applies the current theme to the given activity by setting the status bar
     * and navigation bar colours. Call this in {@code onCreate} before
     * {@code setContentView}.
     */
    public static void applyTheme(Activity activity) {
        Theme theme = loadTheme(activity);
        int primary = getPrimaryColor(theme);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(primary);
            window.setNavigationBarColor(primary);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cube-girl mascot drawable                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Returns the drawable resource ID for the cube-girl mascot matching the
     * given theme name.
     */
    public static int getCubeGirlDrawable(String themeName) {
        Theme theme;
        try {
            theme = Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            theme = Theme.SAKURA;
        }

        switch (theme) {
            case SAKURA:   return R.drawable.cube_girl_sakura;
            case MATCHA:   return R.drawable.cube_girl_matcha;
            case SKY:      return R.drawable.cube_girl_sky;
            case LAVENDER: return R.drawable.cube_girl_lavender;
            case PEACH:    return R.drawable.cube_girl_peach;
            default:       return R.drawable.cube_girl_sakura;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Calendar text color                                                */
    /* ------------------------------------------------------------------ */

    public static int getCalendarTextColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CAL_TEXT_COLOR, DEFAULT_CAL_TEXT_COLOR);
    }

    public static void saveCalendarTextColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_CAL_TEXT_COLOR, color).apply();
    }

    /* ------------------------------------------------------------------ */
    /*  Calendar background color                                         */
    /* ------------------------------------------------------------------ */

    public static int getCalendarBackgroundColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CAL_BG_COLOR, DEFAULT_CAL_BG_COLOR);
    }

    public static void saveCalendarBackgroundColor(Context context, int color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_CAL_BG_COLOR, color).apply();
    }

    /* ------------------------------------------------------------------ */
    /*  Calendar style                                                     */
    /* ------------------------------------------------------------------ */

    public static int getCalendarStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_CAL_STYLE, CAL_STYLE_DEFAULT);
    }

    public static void saveCalendarStyle(Context context, int style) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_CAL_STYLE, style).apply();
    }

    /* ------------------------------------------------------------------ */
    /*  DatePickerDialog style                                             */
    /* ------------------------------------------------------------------ */

    public static int getDatePickerStyle(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_DATE_PICKER_STYLE, DATE_PICKER_STYLE_DEFAULT);
    }

    public static void saveDatePickerStyle(Context context, int style) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_DATE_PICKER_STYLE, style).apply();
    }

    private GhibliTheme() {
        // Utility class — no instances.
    }
}
