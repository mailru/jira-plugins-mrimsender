package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;

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
