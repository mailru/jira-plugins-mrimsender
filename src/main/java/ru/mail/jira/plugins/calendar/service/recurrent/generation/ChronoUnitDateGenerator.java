package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class ChronoUnitDateGenerator extends DateGenerator {
    private final ChronoUnit chronoUnit;
    private final int period;

    public ChronoUnitDateGenerator(ChronoUnit chronoUnit, int period, ZonedDateTime startDate) {
        super(startDate);
        this.chronoUnit = chronoUnit;
        this.period = period;
    }

    @Override
    public void incrementDate() {
        date = date.plus(period, chronoUnit);
    }
}
