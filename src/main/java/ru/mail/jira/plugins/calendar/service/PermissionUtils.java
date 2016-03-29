package ru.mail.jira.plugins.calendar.service;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class PermissionUtils {

    public static String projectRoleSubject(long projectId, long roleId) {
        return String.format("%d-%d", projectId, roleId);
    }

    @Nullable
    public static Long getProject(String subject) {
        try {
            return Long.parseLong(StringUtils.substringBefore(subject, "-"));
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Long getProjectRole(String subject) {
        try {
            return Long.parseLong(StringUtils.substringAfter(subject, "-"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getAccessType(boolean admin, boolean use) {
        if (admin)
            return "ADMIN";
        else if (use)
            return "USE";
        return null;
    }
}
