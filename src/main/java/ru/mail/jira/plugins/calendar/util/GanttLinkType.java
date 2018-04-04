package ru.mail.jira.plugins.calendar.util;

public final class GanttLinkType {
    private GanttLinkType() {}

    public static final String FINISH_TO_START = "0";
    public static final String START_TO_START = "1";
    public static final String FINISH_TO_FINISH = "2";
    public static final String START_TO_FINISH = "3";
}
