package ru.mail.jira.plugins.calendar.model.archive;

import net.java.ao.Entity;
import ru.mail.jira.plugins.calendar.model.Calendar;

@Deprecated
@SuppressWarnings({"deprecation"})
public interface Share extends Entity {
    Calendar getCalendar();
    void setCalendar(Calendar calendar);

    String getGroup();
    void setGroup(String group);

    long getProject();
    void setProject(long project);

    long getRole();
    void setRole(long role);
}
