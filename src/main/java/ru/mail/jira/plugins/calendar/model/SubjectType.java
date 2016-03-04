package ru.mail.jira.plugins.calendar.model;

public enum SubjectType {
    USER, GROUP, PROJECT_ROLE, UNKNOWN;

    public static SubjectType fromInt(int x) {
        switch (x) {
            case 0:
                return USER;
            case 1:
                return GROUP;
            case 2:
                return PROJECT_ROLE;
            default:
                return UNKNOWN;
        }
    }

    public static SubjectType fromString(String type) {
        if ("USER".equals(type))
            return USER;
        else if ("GROUP".equals(type))
            return GROUP;
        else if ("PROJECT_ROLE".equals(type))
            return PROJECT_ROLE;
        return UNKNOWN;
    }
}