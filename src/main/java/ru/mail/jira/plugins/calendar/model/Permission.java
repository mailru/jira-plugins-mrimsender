package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.schema.Table;

@Table("Share")
public interface Permission extends Entity {
    Calendar getCalendar();
    void setCalendar(Calendar calendar);

    PermissionType getPermissionType();
    void setPermissionType(PermissionType permissionType);

    /**
     * Permission subjects:
     * user - [userKey]
     * group - [groupName]
     * project role - [projectId]-[roleId]
     */
    String getPermissionValue();
    void setPermissionValue(String subject);

    boolean isAdmin();
    void setAdmin(boolean admin);



    @Deprecated
    String getGroup();
    @Deprecated
    void setGroup(String group);

    @Deprecated
    long getProject();
    @Deprecated
    void setProject(long project);

    @Deprecated
    long getRole();
    @Deprecated
    void setRole(long role);
}
