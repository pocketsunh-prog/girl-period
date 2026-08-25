package com.girlperiod.app;

import com.girlperiod.app.data.PeriodRecord;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Utility to predict next period, fertile window, and ovulation day.
 * Uses average cycle length (default 28 days) to predict.
 */
public final class PeriodPredictor {

    private PeriodPredictor() {
        throw new AssertionError("No instances.");
    }

    /** Default cycle length in days */
    public static final int DEFAULT_CYCLE_LENGTH = 28;

    /** Default period duration in days */
    public static final int DEFAULT_PERIOD_DURATION = 5;

    /** Minimum number of records needed for reliable prediction */
    public static final int MIN_RECORDS_FOR_PREDICTION = 2;

    /** Fertile window offset from ovulation (days before ovulation) */
    private static final int FERTILE_WINDOW_BEFORE_OVULATION = 5;

    /** Fertile window offset from ovulation (days after ovulation) */
    private static final int FERTILE_WINDOW_AFTER_OVULATION = 1;

    /**
     * Predicts the next period start date based on historical records.
     *
     * @param records list of past period records
     * @return the predicted start date of the next period, or null if insufficient data
     */
    public static Date predictNextPeriod(List<PeriodRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        List<PeriodRecord> sortedRecords = sortRecordsByDate(records);

        if (sortedRecords.size() >= MIN_RECORDS_FOR_PREDICTION) {
            int avgCycleLength = calculateAverageCycleLength(sortedRecords);
            PeriodRecord lastRecord = sortedRecords.get(sortedRecords.size() - 1);
            Date lastStartDate = lastRecord.getStartDate();

            Calendar cal = Calendar.getInstance();
            cal.setTime(lastStartDate);
            cal.add(Calendar.DAY_OF_YEAR, avgCycleLength);
            return cal.getTime();
        } else {
            // Only one record, use default cycle length
            PeriodRecord lastRecord = sortedRecords.get(0);
            Date lastStartDate = lastRecord.getStartDate();

            Calendar cal = Calendar.getInstance();
            cal.setTime(lastStartDate);
            cal.add(Calendar.DAY_OF_YEAR, DEFAULT_CYCLE_LENGTH);
            return cal.getTime();
        }
    }

    /**
     * Returns the fertile window dates based on historical records.
     * The fertile window is typically 5 days before ovulation and 1 day after.
     *
     * @param records list of past period records
     * @return a list of two dates: [fertileWindowStart, fertileWindowEnd], or null if insufficient data
     */
    public static List<Date> getFertileWindow(List<PeriodRecord> records) {
        Date ovulationDay = getOvulationDay(records);
        if (ovulationDay == null) {
            return null;
        }

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(ovulationDay);
        startCal.add(Calendar.DAY_OF_YEAR, -FERTILE_WINDOW_BEFORE_OVULATION);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(ovulationDay);
        endCal.add(Calendar.DAY_OF_YEAR, FERTILE_WINDOW_AFTER_OVULATION);

        List<Date> window = new ArrayList<>();
        window.add(startCal.getTime());
        window.add(endCal.getTime());
        return window;
    }

    /**
     * Returns the estimated ovulation day based on historical records.
     * Ovulation typically occurs 14 days before the next period.
     *
     * @param records list of past period records
     * @return the estimated ovulation date, or null if insufficient data
     */
    public static Date getOvulationDay(List<PeriodRecord> records) {
        Date nextPeriod = predictNextPeriod(records);
        if (nextPeriod == null) {
            return null;
        }

        // Ovulation occurs approximately 14 days before next period
        Calendar cal = Calendar.getInstance();
        cal.setTime(nextPeriod);
        cal.add(Calendar.DAY_OF_YEAR, -14);
        return cal.getTime();
    }

    /**
     * Returns the number of days until the next predicted period.
     *
     * @param records list of past period records
     * @return the number of days until next period, or -1 if insufficient data
     */
    public static int getDaysUntilNextPeriod(List<PeriodRecord> records) {
        Date nextPeriod = predictNextPeriod(records);
        if (nextPeriod == null) {
            return -1;
        }

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar nextCal = Calendar.getInstance();
        nextCal.setTime(nextPeriod);
        nextCal.set(Calendar.HOUR_OF_DAY, 0);
        nextCal.set(Calendar.MINUTE, 0);
        nextCal.set(Calendar.SECOND, 0);
        nextCal.set(Calendar.MILLISECOND, 0);

        long diffMillis = nextCal.getTimeInMillis() - today.getTimeInMillis();
        return (int) (diffMillis / (24 * 60 * 60 * 1000));
    }

    /**
     * Returns the average cycle length calculated from historical records.
     *
     * @param records list of past period records
     * @return the average cycle length in days, or DEFAULT_CYCLE_LENGTH if insufficient data
     */
    public static int getAverageCycleLength(List<PeriodRecord> records) {
        if (records == null || records.size() < MIN_RECORDS_FOR_PREDICTION) {
            return DEFAULT_CYCLE_LENGTH;
        }

        List<PeriodRecord> sortedRecords = sortRecordsByDate(records);
        return calculateAverageCycleLength(sortedRecords);
    }

    /**
     * Returns the average period duration calculated from historical records.
     *
     * @param records list of past period records
     * @return the average period duration in days, or DEFAULT_PERIOD_DURATION if insufficient data
     */
    public static int getAveragePeriodDuration(List<PeriodRecord> records) {
        if (records == null || records.isEmpty()) {
            return DEFAULT_PERIOD_DURATION;
        }

        int totalDuration = 0;
        int count = 0;

        for (PeriodRecord record : records) {
            if (record.getDuration() > 0) {
                totalDuration += record.getDuration();
                count++;
            }
        }

        if (count == 0) {
            return DEFAULT_PERIOD_DURATION;
        }

        return Math.round((float) totalDuration / count);
    }

    /**
     * Returns the current cycle day (day number within the current cycle).
     * Day 1 is the first day of the most recent period.
     *
     * @param records list of past period records
     * @return the current cycle day, or -1 if insufficient data
     */
    public static int getCurrentCycleDay(List<PeriodRecord> records) {
        if (records == null || records.isEmpty()) {
            return -1;
        }

        List<PeriodRecord> sortedRecords = sortRecordsByDate(records);
        PeriodRecord lastRecord = sortedRecords.get(sortedRecords.size() - 1);
        Date lastStartDate = lastRecord.getStartDate();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(lastStartDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        long diffMillis = today.getTimeInMillis() - startCal.getTimeInMillis();
        return (int) (diffMillis / (24 * 60 * 60 * 1000)) + 1;
    }

    /**
     * Returns a list of predicted period dates for the next N cycles.
     *
     * @param records  list of past period records
     * @param numCycles number of future cycles to predict
     * @return a list of predicted period start dates
     */
    public static List<Date> predictNextPeriods(List<PeriodRecord> records, int numCycles) {
        if (records == null || records.isEmpty() || numCycles <= 0) {
            return Collections.emptyList();
        }

        List<Date> predictions = new ArrayList<>();
        List<PeriodRecord> sortedRecords = sortRecordsByDate(records);
        int avgCycleLength = calculateAverageCycleLength(sortedRecords);

        PeriodRecord lastRecord = sortedRecords.get(sortedRecords.size() - 1);
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastRecord.getStartDate());

        for (int i = 0; i < numCycles; i++) {
            cal.add(Calendar.DAY_OF_YEAR, avgCycleLength);
            predictions.add(cal.getTime());
        }

        return predictions;
    }

    /**
     * Checks if the given date falls within the fertile window.
     *
     * @param records list of past period records
     * @param date    the date to check
     * @return true if the date is within the fertile window
     */
    public static boolean isFertileDay(List<PeriodRecord> records, Date date) {
        List<Date> window = getFertileWindow(records);
        if (window == null || window.size() < 2) {
            return false;
        }

        Calendar checkCal = Calendar.getInstance();
        checkCal.setTime(date);
        checkCal.set(Calendar.HOUR_OF_DAY, 0);
        checkCal.set(Calendar.MINUTE, 0);
        checkCal.set(Calendar.SECOND, 0);
        checkCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(window.get(0));
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(window.get(1));
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        long checkTime = checkCal.getTimeInMillis();
        return checkTime >= startCal.getTimeInMillis() && checkTime <= endCal.getTimeInMillis();
    }

    /**
     * Checks if the given date is a predicted period day.
     *
     * @param records list of past period records
     * @param date    the date to check
     * @return true if the date is within the predicted period window
     */
    public static boolean isPredictedPeriodDay(List<PeriodRecord> records, Date date) {
        Date nextPeriod = predictNextPeriod(records);
        if (nextPeriod == null) {
            return false;
        }

        int periodDuration = getAveragePeriodDuration(records);

        Calendar checkCal = Calendar.getInstance();
        checkCal.setTime(date);
        checkCal.set(Calendar.HOUR_OF_DAY, 0);
        checkCal.set(Calendar.MINUTE, 0);
        checkCal.set(Calendar.SECOND, 0);
        checkCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(nextPeriod);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(nextPeriod);
        endCal.add(Calendar.DAY_OF_YEAR, periodDuration - 1);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        long checkTime = checkCal.getTimeInMillis();
        return checkTime >= startCal.getTimeInMillis() && checkTime <= endCal.getTimeInMillis();
    }

    // --- Private helper methods ---

    private static List<PeriodRecord> sortRecordsByDate(List<PeriodRecord> records) {
        List<PeriodRecord> sorted = new ArrayList<>(records);
        Collections.sort(sorted, new Comparator<PeriodRecord>() {
            @Override
            public int compare(PeriodRecord r1, PeriodRecord r2) {
                return r1.getStartDate().compareTo(r2.getStartDate());
            }
        });
        return sorted;
    }

    private static int calculateAverageCycleLength(List<PeriodRecord> sortedRecords) {
        if (sortedRecords.size() < MIN_RECORDS_FOR_PREDICTION) {
            return DEFAULT_CYCLE_LENGTH;
        }

        int totalCycleDays = 0;
        int cycleCount = 0;

        for (int i = 1; i < sortedRecords.size(); i++) {
            Date prevStart = sortedRecords.get(i - 1).getStartDate();
            Date currStart = sortedRecords.get(i).getStartDate();

            Calendar prevCal = Calendar.getInstance();
            prevCal.setTime(prevStart);
            prevCal.set(Calendar.HOUR_OF_DAY, 0);
            prevCal.set(Calendar.MINUTE, 0);
            prevCal.set(Calendar.SECOND, 0);
            prevCal.set(Calendar.MILLISECOND, 0);

            Calendar currCal = Calendar.getInstance();
            currCal.setTime(currStart);
            currCal.set(Calendar.HOUR_OF_DAY, 0);
            currCal.set(Calendar.MINUTE, 0);
            currCal.set(Calendar.SECOND, 0);
            currCal.set(Calendar.MILLISECOND, 0);

            long diffMillis = currCal.getTimeInMillis() - prevCal.getTimeInMillis();
            int diffDays = (int) (diffMillis / (24 * 60 * 60 * 1000));

            // Only count reasonable cycle lengths (21-35 days)
            if (diffDays >= 21 && diffDays <= 35) {
                totalCycleDays += diffDays;
                cycleCount++;
            }
        }

        if (cycleCount == 0) {
            return DEFAULT_CYCLE_LENGTH;
        }

        return Math.round((float) totalCycleDays / cycleCount);
    }
}
