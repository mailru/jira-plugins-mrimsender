package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;

public interface EventTypeReminder extends Entity {
    @NotNull
    void setEventType(EventType eventType);

    EventType getEventType();

    @NotNull
    void setCalendarId(int calendarId);

    int getCalendarId();

    @NotNull
    void setReminderType(ReminderType reminderType);

    ReminderType getReminderType();
}
