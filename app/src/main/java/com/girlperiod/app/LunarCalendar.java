package com.girlperiod.app;

import java.util.Calendar;
import java.util.Date;

/**
 * Utility class that converts Gregorian date to Chinese Lunar calendar date.
 * Uses a simplified lunar calendar calculation with lookup tables for lunar data.
 */
public final class LunarCalendar {

    private LunarCalendar() {
        throw new AssertionError("No instances.");
    }

    // Lunar data lookup tables (simplified, covering 1900-2100)
    // Each entry is a 4-digit hex number representing the lunar month lengths
    // For example: 0x0c960 = 0000 1100 1001 0110 0000 (binary)
    // Each bit represents a month: 1 = 30 days, 0 = 29 days
    private static final int[] LUNAR_INFO = {
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520
    };

    private static final String[] LUNAR_MONTH_NAMES = {
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    };

    private static final String[] LUNAR_DAY_NAMES = {
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };

    private static final String[] ZODIAC_ANIMALS = {
        "鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"
    };

    private static final String[] HEAVENLY_STEMS = {
        "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"
    };

    private static final String[] EARTHLY_BRANCHES = {
        "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"
    };

    private static final int BASE_YEAR = 1900;
    private static final long BASE_DATE_TIMESTAMP;

    static {
        Calendar baseCal = Calendar.getInstance();
        baseCal.set(BASE_YEAR, 0, 31, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);
        BASE_DATE_TIMESTAMP = baseCal.getTimeInMillis();
    }

    /**
     * Converts a Gregorian date to a Chinese Lunar calendar date string.
     *
     * @param year  the Gregorian year
     * @param month the Gregorian month (1-12)
     * @param day   the Gregorian day (1-31)
     * @return a string like "农历正月初一"
     */
    public static String getLunarDate(int year, int month, int day) {
        int offset = getDaysBetweenGregorianAndBase(year, month, day);
        int lunarYear = BASE_YEAR;
        int daysInYear;

        // Find the lunar year
        for (int i = BASE_YEAR; i < 2101 && offset > 0; i++) {
            daysInYear = getLunarYearDays(i);
            if (offset < daysInYear) {
                lunarYear = i;
                break;
            }
            offset -= daysInYear;
        }

        int leapMonth = getLeapMonth(lunarYear);
        boolean isLeap = false;
        int lunarMonth = 1;
        int lunarDay;

        // Find the lunar month and day
        for (int i = 1; i < 13 && offset > 0; i++) {
            int daysInMonth;
            if (leapMonth > 0 && i == (leapMonth + 1) && !isLeap) {
                --i;
                isLeap = true;
                daysInMonth = getLeapDays(lunarYear);
            } else {
                daysInMonth = getLunarMonthDays(lunarYear, i);
            }

            if (isLeap && i == (leapMonth + 1)) {
                isLeap = false;
            }

            if (offset < daysInMonth) {
                lunarMonth = i;
                break;
            }
            offset -= daysInMonth;
        }

        lunarDay = offset + 1;

        return "农历" + getLunarMonthName(lunarMonth) + getLunarDayName(lunarMonth, lunarDay);
    }

    /**
     * Returns the lunar day name for the given lunar month and day.
     *
     * @param lunarMonth the lunar month (1-12)
     * @param lunarDay   the lunar day (1-30)
     * @return the lunar day name like "初一", "十五", etc.
     */
    public static String getLunarDayName(int lunarMonth, int lunarDay) {
        if (lunarDay < 1 || lunarDay > 30) {
            return "未知";
        }
        return LUNAR_DAY_NAMES[lunarDay - 1];
    }

    /**
     * Returns the lunar month name for the given lunar month.
     *
     * @param lunarMonth the lunar month (1-12)
     * @return the lunar month name like "正月", "二月", etc.
     */
    public static String getLunarMonthName(int lunarMonth) {
        if (lunarMonth < 1 || lunarMonth > 12) {
            return "未知";
        }
        return LUNAR_MONTH_NAMES[lunarMonth - 1];
    }

    /**
     * Returns the Chinese zodiac animal for the given year.
     *
     * @param year the Gregorian year
     * @return the zodiac animal like "鼠", "牛", etc.
     */
    public static String getZodiacSign(int year) {
        int index = (year - 4) % 12;
        if (index < 0) {
            index += 12;
        }
        return ZODIAC_ANIMALS[index];
    }

    /**
     * Returns the full Chinese zodiac name (heavenly stem + earthly branch + animal).
     *
     * @param year the Gregorian year
     * @return the full zodiac name like "甲子鼠"
     */
    public static String getFullZodiac(int year) {
        int stemIndex = (year - 4) % 10;
        int branchIndex = (year - 4) % 12;
        if (stemIndex < 0) stemIndex += 10;
        if (branchIndex < 0) branchIndex += 12;
        return HEAVENLY_STEMS[stemIndex] + EARTHLY_BRANCHES[branchIndex] + getZodiacSign(year);
    }

    // --- Private helper methods ---

    private static int getDaysBetweenGregorianAndBase(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long diff = cal.getTimeInMillis() - BASE_DATE_TIMESTAMP;
        return (int) (diff / (24 * 60 * 60 * 1000));
    }

    private static int getLunarYearDays(int year) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            sum += (LUNAR_INFO[year - BASE_YEAR] & i) != 0 ? 1 : 0;
        }
        return sum + getLeapDays(year);
    }

    private static int getLeapDays(int year) {
        if (getLeapMonth(year) != 0) {
            return (LUNAR_INFO[year - BASE_YEAR] & 0x10000) != 0 ? 30 : 29;
        }
        return 0;
    }

    private static int getLeapMonth(int year) {
        return LUNAR_INFO[year - BASE_YEAR] & 0xf;
    }

    private static int getLunarMonthDays(int year, int month) {
        return (LUNAR_INFO[year - BASE_YEAR] & (0x10000 >> month)) != 0 ? 30 : 29;
    }
}
