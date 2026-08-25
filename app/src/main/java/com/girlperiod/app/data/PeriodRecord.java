package com.girlperiod.app.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PeriodRecord {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private long id;
    private long userId;
    private String startDate;
    private String endDate;
    private int cycleLength;
    private String notes;

    public PeriodRecord() {
    }

    public PeriodRecord(long id, long userId, String startDate, String endDate, int cycleLength, String notes) {
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cycleLength = cycleLength;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * Returns the start date as a Date object (for PeriodPredictor compatibility).
     */
    public Date getStartDate() {
        try {
            return DATE_FORMAT.parse(startDate);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Sets the start date from a Date object.
     */
    public void setStartDate(Date startDate) {
        this.startDate = DATE_FORMAT.format(startDate);
    }

    /**
     * Sets the start date from a String.
     */
    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the raw start date string (for DB operations).
     */
    public String getStartDateString() {
        return startDate;
    }

    /**
     * Returns the end date as a Date object.
     */
    public Date getEndDate() {
        try {
            return DATE_FORMAT.parse(endDate);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Sets the end date from a Date object.
     */
    public void setEndDate(Date endDate) {
        this.endDate = DATE_FORMAT.format(endDate);
    }

    /**
     * Sets the end date from a String.
     */
    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns the raw end date string (for DB operations).
     */
    public String getEndDateString() {
        return endDate;
    }

    public int getCycleLength() {
        return cycleLength;
    }

    public void setCycleLength(int cycleLength) {
        this.cycleLength = cycleLength;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns the period duration in days.
     */
    public int getDuration() {
        Date start = getStartDate();
        Date end = getEndDate();
        if (start != null && end != null) {
            long diff = end.getTime() - start.getTime();
            return (int) (diff / (24 * 60 * 60 * 1000)) + 1;
        }
        return 0;
    }

    /**
     * Returns the start date as a Date object (alias for getStartDate).
     */
    public Date getStartDateDate() {
        return getStartDate();
    }

    /**
     * Returns the end date as a Date object (alias for getEndDate).
     */
    public Date getEndDateDate() {
        return getEndDate();
    }
}
