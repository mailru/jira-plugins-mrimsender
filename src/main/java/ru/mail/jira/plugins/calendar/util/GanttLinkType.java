package ru.mail.jira.plugins.calendar.util;

public enum GanttLinkType {
    FINISH_TO_START,
    START_TO_START,
    FINISH_TO_FINISH,
    START_TO_FINISH;

    public static GanttLinkType fromString(String string) {
        switch (string) {
            case "0":
                return FINISH_TO_START;
            case "1":
                return START_TO_START;
            case "2":
                return FINISH_TO_FINISH;
            case "3":
                return START_TO_FINISH;
        }
        return null;
    }
}
