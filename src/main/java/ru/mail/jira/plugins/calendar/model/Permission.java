package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;

public interface Permission extends Entity {
    Calendar getCalendar();
    void setCalendar(Calendar calendar);

    int getSubjectType();
    void setSubjectType(int subjectType);

    /**
     * Permission subjects:
     * user - [userKey]
     * group - [groupName]
     * project role - [projectId]-[roleId]
     */
    String getSubject();
    void setSubject(String subject);

    boolean isUse();
    void setUse(boolean use);

    boolean isAdmin();
    void setAdmin(boolean admin);
}
