package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;

public interface GanttLink extends Entity {
    @Indexed
    int getCalendarId();
    void setCalendarId(int calendarId);

    @Indexed
    String getSource();
    void setSource(String source);

    @Indexed
    String getTarget();
    void setTarget(String target);

    String getType();
    void setType(String type);
}
