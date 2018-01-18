package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.OneToMany;
import net.java.ao.schema.StringLength;

public interface Calendar extends Entity {
    String getName();
    void setName(String name);

    String getColor();
    void setColor(String color);

    @StringLength(StringLength.UNLIMITED)
    String getSource();
    void setSource(String source);

    String getEventStart();
    void setEventStart(String eventStart);

    String getEventEnd();
    void setEventEnd(String eventEnd);

    @StringLength(StringLength.UNLIMITED)
    String getDisplayedFields();
    void setDisplayedFields(String displayedFields);

    boolean isShowIssueStatus();
    void setShowIssueStatus(boolean showIssueStatus);

    @OneToMany
    @Accessor("Share")
    Permission[] getPermissions();

    boolean isGanttEnabled();
    void setGanttEnabled(boolean ganttEnabled);

    String getEventDurationField();
    void setEventDurationField(String eventDurationField);

    String getEventProgressField();
    void setEventProgressField(String eventProgressField);

    String getEventParentField();
    void setEventParentField(String eventParentField);

    @Deprecated
    String getAuthorKey();
    @Deprecated
    void setAuthorKey(String authorKey);
}