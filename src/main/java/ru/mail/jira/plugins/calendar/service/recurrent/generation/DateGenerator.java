package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import java.time.ZonedDateTime;

public abstract class DateGenerator {
    protected ZonedDateTime date;

    public DateGenerator(ZonedDateTime date) {
        this.date = date;
    }

    public ZonedDateTime nextDate() {
        ZonedDateTime result = date;
        incrementDate();
        return result;
    }

    protected abstract void incrementDate();
}
