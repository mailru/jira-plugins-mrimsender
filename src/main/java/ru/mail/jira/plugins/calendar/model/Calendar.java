package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.OneToMany;
import ru.mail.jira.plugins.calendar.model.archive.Share;

public interface Calendar extends Entity {
    String getName();
    void setName(String name);

    String getAuthorKey();
    void setAuthorKey(String authorKey);

    String getColor();
    void setColor(String color);

    String getSource();
    void setSource(String source);

    @OneToMany
    @Deprecated
    Share[] getShares();

    String getEventStart();
    void setEventStart(String eventStart);

    String getEventEnd();
    void setEventEnd(String eventEnd);

    String getDisplayedFields();
    void setDisplayedFields(String displayedFields);

    @OneToMany
    Permission[] getPermissions();
}