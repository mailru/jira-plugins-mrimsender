package ru.mail.jira.plugins.calendar.model;

public enum PermissionType {
    USER, GROUP, PROJECT_ROLE;

    public static PermissionType fromString(String type) {
        if ("USER".equals(type))
            return USER;
        else if ("GROUP".equals(type))
            return GROUP;
        else if ("PROJECT_ROLE".equals(type))
            return PROJECT_ROLE;
        throw new IllegalArgumentException("Can't parse PermissionType");
    }
}