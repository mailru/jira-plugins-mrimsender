package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.Unique;

/**
 * Created by i.mashintsev on 20.11.15.
 */
public interface CalendarFeed extends Entity {

    @Unique
    @NotNull
    String getUserKey();
    void setUserKey(String userKey);

    @NotNull
    String getUid();
    void setUid(String uid);
}
