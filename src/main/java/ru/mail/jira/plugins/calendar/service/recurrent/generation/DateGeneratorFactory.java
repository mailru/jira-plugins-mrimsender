package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import com.google.common.collect.ImmutableSet;
import ru.mail.jira.plugins.calendar.model.Event;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class DateGeneratorFactory {
    private DateGeneratorFactory() {};

    public static DateGenerator getDateGenerator(Event event, ZoneId zoneId) {
        ZonedDateTime startDate = event.getStartDate().toInstant().atZone(zoneId);
        switch (event.getRecurrenceType()) {
            case DAILY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.DAYS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case WEEKDAYS:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case MON_WED_FRI:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case TUE_THU:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.TUESDAY,
                        DayOfWeek.THURSDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case DAYS_OF_WEEK:
                return new DaysOfWeekDateGenerator(
                    Arrays
                        .stream(event.getRecurrenceExpression().split(","))
                        .map(DayOfWeek::valueOf)
                        .collect(Collectors.toSet()),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case MONTHLY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.MONTHS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case YEARLY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.YEARS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case CRON:
                return new CronDateGenerator(
                    event.getRecurrenceExpression(),
                    zoneId,
                    startDate
                );
        }
        return null;
    }
}
