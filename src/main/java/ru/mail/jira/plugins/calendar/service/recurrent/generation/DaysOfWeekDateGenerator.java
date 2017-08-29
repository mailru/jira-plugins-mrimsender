package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Set;

public class DaysOfWeekDateGenerator extends DateGenerator {
    private final Set<DayOfWeek> daysOfWeek;
    private final int period;

    public DaysOfWeekDateGenerator(Set<DayOfWeek> daysOfWeek, int period, ZonedDateTime startDate) {
        super(startDate);
        this.daysOfWeek = daysOfWeek;
        this.period = period-1;
    }

    @Override
    protected void incrementDate() {
        doIncrement();
        while (!daysOfWeek.contains(date.getDayOfWeek())) {
            doIncrement();
        }
    }

    private void doIncrement() {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(period * 7 + 1);
        } else {
            date = date.plusDays(1);
        }
    }
}
