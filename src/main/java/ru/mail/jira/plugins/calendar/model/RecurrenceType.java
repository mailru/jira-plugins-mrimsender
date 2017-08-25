package ru.mail.jira.plugins.calendar.model;

import ru.mail.jira.plugins.calendar.service.recurrent.validation.*;

public enum RecurrenceType {
    DAILY(new ChronoUnitRecurrenceValidator()),
    WEEKDAYS(new FixedDaysOfWeekRecurrenceValidator()),
    MON_WED_FRI(new FixedDaysOfWeekRecurrenceValidator()),
    TUE_THU(new FixedDaysOfWeekRecurrenceValidator()),
    DAYS_OF_WEEK(new CustomDaysOfWeekRecurrenceValidator()),
    MONTHLY(new ChronoUnitRecurrenceValidator()),
    YEARLY(new ChronoUnitRecurrenceValidator()),
    CRON(new CronRecurrenceValidator());

    private final RecurrenceValidator validator;

    RecurrenceType(RecurrenceValidator validator) {
        this.validator = validator;
    }

    public RecurrenceValidator getValidator() {
        return validator;
    }
}
