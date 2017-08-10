package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;

public interface EventType extends Entity {
    void setCalendarId(Integer calendarId);

    Integer getCalendarId();

    void setName(String name);

    String getName();

    void setI18nName(String i18nName);

    String getI18nName();

    void setSystem(boolean system);

    boolean isSystem();

    @NotNull
    void setDeleted(boolean deleted);

    boolean isDeleted();

    void setAvatar(String avatar);

    String getAvatar();
}
