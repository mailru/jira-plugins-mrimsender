package ru.mail.jira.plugins.calendar.model.gantt;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;
import net.java.ao.schema.Table;

@Table("TEAM")
public interface GanttTeam extends Entity {
    String getName();
    void setName(String name);

    @OneToMany
    GanttUser[] getUsers();

    int getCalendarId();
    void setCalendarId(int calendarId);
}