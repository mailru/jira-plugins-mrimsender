package ru.mail.jira.plugins.calendar.common;

import java.util.TimeZone;

public class Consts {
    public static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    public static final String SCHEDULE_PREFIX = "schedule";
    public static final String SCHEDULE_ID = "ru.mail.jira.plugins.calendar.schedule:scheduleId";

    public static final String SR_FIELD_KEY = "com.onresolve.jira.groovy.groovyrunner:scripted-field";
    public static final String SR_DATE_SEARCHER_KEY = "com.onresolve.jira.groovy.groovyrunner:datetimerange";
}
