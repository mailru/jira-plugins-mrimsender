package ru.mail.jira.plugins.calendar.model;

import java.util.concurrent.TimeUnit;

public enum ReminderType {
    MINUTES_5(TimeUnit.MINUTES.toMillis(5)),
    MINUTES_10(TimeUnit.MINUTES.toMillis(10)),
    MINUTES_15(TimeUnit.MINUTES.toMillis(15)),
    MINUTES_30(TimeUnit.MINUTES.toMillis(30)),
    HOURS_1(TimeUnit.HOURS.toMillis(1)),
    HOURS_8(TimeUnit.HOURS.toMillis(8)),
    DAYS_1(TimeUnit.DAYS.toMillis(1)),
    WEEKS_1(TimeUnit.DAYS.toMillis(7));

    private final long durationMillis;

    ReminderType(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
