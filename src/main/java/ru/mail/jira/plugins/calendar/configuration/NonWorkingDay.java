package ru.mail.jira.plugins.calendar.configuration;

import net.java.ao.Entity;

import java.util.Date;

public interface NonWorkingDay extends Entity {
    Date getDate();
    void setDate(Date date);

    String getDescription();
    void setDescription(String description);
}
