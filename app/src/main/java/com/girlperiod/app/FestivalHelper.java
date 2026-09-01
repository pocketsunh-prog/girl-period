package com.girlperiod.app;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to determine festival/holiday dates.
 * Supports both fixed date festivals and lunar calendar festivals.
 */
public class FestivalHelper {

    private static final Map<String, String> FIXED_FESTIVALS = new HashMap<>();
    private static final Map<String, String> LUNAR_FESTIVALS = new HashMap<>();

    static {
        // Fixed date festivals (MM-dd format)
        FIXED_FESTIVALS.put("01-01", "New Year");
        FIXED_FESTIVALS.put("02-14", "Valentine's Day");
        FIXED_FESTIVALS.put("03-08", "Women's Day");
        FIXED_FESTIVALS.put("04-01", "April Fools' Day");
        FIXED_FESTIVALS.put("05-01", "Labor Day");
        FIXED_FESTIVALS.put("06-01", "Children's Day");
        FIXED_FESTIVALS.put("10-01", "National Day");
        FIXED_FESTIVALS.put("10-31", "Halloween");
        FIXED_FESTIVALS.put("12-24", "Christmas Eve");
        FIXED_FESTIVALS.put("12-25", "Christmas");
        FIXED_FESTIVALS.put("12-31", "New Year's Eve");

        // Lunar festivals (lunar month-day format)
        LUNAR_FESTIVALS.put("01-01", "Spring Festival");
        LUNAR_FESTIVALS.put("01-15", "Lantern Festival");
        LUNAR_FESTIVALS.put("05-05", "Dragon Boat Festival");
        LUNAR_FESTIVALS.put("07-07", "Qixi Festival");
        LUNAR_FESTIVALS.put("08-15", "Mid-Autumn Festival");
        LUNAR_FESTIVALS.put("09-09", "Double Ninth Festival");
        LUNAR_FESTIVALS.put("12-08", "Laba Festival");
        LUNAR_FESTIVALS.put("12-23", "Kitchen God Festival");
        LUNAR_FESTIVALS.put("12-30", "New Year's Eve");
    }

    /**
     * Check if a date is a festival.
     * @param year The year
     * @param month The month (1-12)
     * @param day The day
     * @return Festival name or null if not a festival
     */
    public static String getFestival(int year, int month, int day) {
        // Check fixed festivals first
        String key = String.format("%02d-%02d", month, day);
        if (FIXED_FESTIVALS.containsKey(key)) {
            return FIXED_FESTIVALS.get(key);
        }

        // Check lunar festivals
        String lunarDate = LunarCalendar.getLunarDate(year, month, day);
        if (lunarDate != null && lunarDate.length() > 4) {
            // Extract lunar month and day from format "农历正月初一"
            try {
                String lunarMonthDay = lunarDate.substring(2); // Remove "农历"
                for (Map.Entry<String, String> entry : LUNAR_FESTIVALS.entrySet()) {
                    String festivalKey = entry.getKey();
                    String festivalName = entry.getValue();
                    // Check if lunar date matches festival
                    if (lunarMonthDay.equals(getLunarMonthDayName(festivalKey))) {
                        return festivalName;
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors
            }
        }

        return null;
    }

    /**
     * Get the lunar month-day name for a given key.
     */
    private static String getLunarMonthDayName(String key) {
        // Convert "01-01" to "正月初一" format
        try {
            String[] parts = key.split("-");
            int month = Integer.parseInt(parts[0]);
            int day = Integer.parseInt(parts[1]);
            return getLunarMonth(month) + getLunarDay(day);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getLunarMonth(int month) {
        String[] months = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
        if (month >= 1 && month <= 12) {
            return months[month - 1] + "月";
        }
        return "";
    }

    private static String getLunarDay(int day) {
        String[] days = {"初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};
        if (day >= 1 && day <= 30) {
            return days[day - 1];
        }
        return "";
    }

    /**
     * Get the icon resource for a festival.
     */
    public static int getFestivalIcon(String festivalName) {
        if (festivalName == null) return 0;
        switch (festivalName) {
            case "New Year":
            case "New Year's Eve":
                return R.drawable.ic_festival_new_year;
            case "Valentine's Day":
                return R.drawable.ic_festival_valentine;
            case "Women's Day":
                return R.drawable.ic_festival_women;
            case "April Fools' Day":
                return R.drawable.ic_festival_april_fools;
            case "Labor Day":
                return R.drawable.ic_festival_labor;
            case "Children's Day":
                return R.drawable.ic_festival_children;
            case "National Day":
                return R.drawable.ic_festival_national;
            case "Halloween":
                return R.drawable.ic_festival_halloween;
            case "Christmas":
            case "Christmas Eve":
                return R.drawable.ic_festival_christmas;
            case "Spring Festival":
                return R.drawable.ic_festival_spring;
            case "Lantern Festival":
                return R.drawable.ic_festival_lantern;
            case "Dragon Boat Festival":
                return R.drawable.ic_festival_dragon_boat;
            case "Qixi Festival":
                return R.drawable.ic_festival_qixi;
            case "Mid-Autumn Festival":
                return R.drawable.ic_festival_mid_autumn;
            case "Double Ninth Festival":
                return R.drawable.ic_festival_double_ninth;
            case "Laba Festival":
                return R.drawable.ic_festival_laba;
            default:
                return R.drawable.ic_festival_generic;
        }
    }
}
