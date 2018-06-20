package ru.mail.jira.plugins.calendar.util;

import ru.mail.jira.plugins.calendar.service.CalendarEventService;

public final class FieldUtil {
    private FieldUtil() {}

    public static String getFieldId(String fieldKey) {
        if (CalendarEventService.DUE_DATE_KEY.equals(fieldKey)) {
            return "duedate";
        } else if (CalendarEventService.RESOLVED_DATE_KEY.equals(fieldKey)) {
            return "resolutiondate";
        }

        return fieldKey;
    }
}
