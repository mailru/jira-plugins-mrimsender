package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.Table;


@Table("EXPORTED_CALENDARS")
public interface ExportedCalendarsData extends Entity {
    @Indexed
    String getIcalUid();
    void setICalUid(String uid);

    String getCalendarIds();
    void setCalendarIds(String calendarIds);
}
