package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import org.springframework.scheduling.support.CronSequenceGenerator;

import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class CronDateGenerator extends DateGenerator {
    private final CronSequenceGenerator cronSequenceGenerator;
    private final ZoneId zoneId;

    public CronDateGenerator(String cronExpression, ZoneId zoneId, ZonedDateTime startDate) {
        super(startDate);
        this.zoneId = zoneId;
        this.cronSequenceGenerator = new CronSequenceGenerator(cronExpression, TimeZone.getTimeZone(zoneId));
    }

    @Override
    protected void incrementDate() {
        date = cronSequenceGenerator.next(Date.from(date.toInstant())).toInstant().atZone(zoneId);
    }
}
