package ru.mail.jira.plugins.calendar.model.gantt;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("USER_TEAM")
public interface GanttUserToGanttTeam extends Entity {
    GanttUser getUser();
    void setUser(GanttUser user);

    GanttTeam getTeam();
    void setTeam(GanttTeam team);

    Integer getWeeklyHours();
    void setWeeklyHours(Integer weeklyHours);
}
