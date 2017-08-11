package ru.mail.jira.plugins.calendar.schedule.model;

import net.java.ao.Entity;
import net.java.ao.schema.Default;

import java.util.Date;

public interface Schedule extends Entity {
    long getSourceIssueId();
    void setSourceIssueId(long sourceIssueId);

    String getName();
    void setName(String name);

    String getCreatorKey();
    void setCreatorKey(String creatorKey);

    String getMode();
    void setMode(String mode);

    String getCronExpression();
    void setCronExpression(String paramString);

    @Default("0")
    int getRunCount();
    void setRunCount(int runCount);

    Date getLastRun();
    void setLastRun(Date lastRun);

    long getLastCreatedIssueId();
    void setLastCreatedIssueId(long lastCreatedIssueId);

    boolean isDeleted();
    void setDeleted(boolean deleted);
}
