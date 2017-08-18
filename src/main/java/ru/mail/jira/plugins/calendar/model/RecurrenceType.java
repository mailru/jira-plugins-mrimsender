package ru.mail.jira.plugins.calendar.model;

import com.google.common.collect.ImmutableSet;
import ru.mail.jira.plugins.calendar.service.recurrent.*;

import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;

public enum RecurrenceType {
    DAILY(new ChronoUnitRecurrenceStrategy(ChronoUnit.DAYS)),
    WEEKDAYS(new FixedDaysOfWeekRecurrenceStrategy(ImmutableSet.of(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    ))),
    MON_WED_FRI(new FixedDaysOfWeekRecurrenceStrategy(ImmutableSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))),
    TUE_THU(new FixedDaysOfWeekRecurrenceStrategy(ImmutableSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY))),
    DAYS_OF_WEEK(new CustomDaysOfWeekRecurrenceStrategy()),
    WEEKLY(new ChronoUnitRecurrenceStrategy(ChronoUnit.WEEKS)),
    MONTHLY(new ChronoUnitRecurrenceStrategy(ChronoUnit.MONTHS)),
    YEARLY(new ChronoUnitRecurrenceStrategy(ChronoUnit.YEARS)),
    CRON(new CronRecurrenceStrategy());

    private final RecurrenceStrategy recurrenceStrategy;

    RecurrenceType(RecurrenceStrategy recurrenceStrategy) {
        this.recurrenceStrategy = recurrenceStrategy;
    }

    public RecurrenceStrategy getRecurrenceStrategy() {
        return recurrenceStrategy;
    }
}
