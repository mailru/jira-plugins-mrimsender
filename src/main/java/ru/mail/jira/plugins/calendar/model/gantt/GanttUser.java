package ru.mail.jira.plugins.calendar.model.gantt;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("USER")
public interface GanttUser extends Entity {
    String getKey();
    void setKey(String key);

    Integer getWeeklyHours();
    void setWeeklyHours(Integer weeklyHours);

    GanttTeam getTeam();
    void setTeam(GanttTeam team);
}
