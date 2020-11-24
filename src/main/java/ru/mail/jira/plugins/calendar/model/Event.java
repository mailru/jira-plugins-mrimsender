package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.NotNull;

import java.sql.Timestamp;

public interface Event extends Entity {
    @NotNull
    @Indexed
    void setCalendarId(int calendarId);

    int getCalendarId();

    @NotNull
    void setCreatorKey(String creatorKey);

    String getCreatorKey();

    @NotNull
    void setTitle(String title);

    String getTitle();

    @NotNull
    void setEventType(EventType eventType);

    EventType getEventType();

    void setParticipants(String users);

    String getParticipants();

    @NotNull
    void setAllDay(boolean allDay);

    boolean isAllDay();

    @NotNull
    @Indexed
    void setStartDate(Timestamp startDate);

    Timestamp getStartDate();

    @Indexed
    void setEndDate(Timestamp endDate);

    Timestamp getEndDate();

    void setParent(Event event);

    Event getParent();

    void setRecurrenceType(RecurrenceType recurrenceType);

    RecurrenceType getRecurrenceType();

    void setRecurrenceExpression(String expression);

    String getRecurrenceExpression();

    void setRecurrencePeriod(Integer recurrencePeriod);

    Integer getRecurrencePeriod();

    void setRecurrenceEndDate(Timestamp recurrenceEndDate);

    Timestamp getRecurrenceEndDate();

    void setRecurrenceCount(Integer recurrenceCount);

    Integer getRecurrenceCount();

    void setRecurrenceNumber(Integer recurrenceNumber);

    Integer getRecurrenceNumber();
}
