package com.girlperiod.app.data;

public class Event {

    private long id;
    private long userId;
    private String title;
    private String eventDate;
    private String notes;
    private int reminderDays;

    public Event() {
    }

    public Event(long id, long userId, String title, String eventDate, String notes, int reminderDays) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.eventDate = eventDate;
        this.notes = notes;
        this.reminderDays = reminderDays;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getReminderDays() {
        return reminderDays;
    }

    public void setReminderDays(int reminderDays) {
        this.reminderDays = reminderDays;
    }
}
