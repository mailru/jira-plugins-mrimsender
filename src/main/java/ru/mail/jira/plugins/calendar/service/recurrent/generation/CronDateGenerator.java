package ru.mail.jira.plugins.calendar.service.recurrent.generation;

import com.atlassian.scheduler.caesium.cron.parser.CronExpressionParser;
import com.atlassian.scheduler.caesium.cron.rule.CronExpression;
import com.atlassian.scheduler.caesium.cron.rule.DateTimeTemplate;
import com.atlassian.scheduler.cron.CronSyntaxException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class CronDateGenerator extends DateGenerator {
    private final ZoneId zoneId;
    private final CronExpression cronExpression;
    private DateTimeTemplate dateTimeTemplate;

    public CronDateGenerator(String cronExpression, ZoneId zoneId, ZonedDateTime startDate) throws CronSyntaxException {
        super(startDate);
        this.zoneId = zoneId;
        this.cronExpression = CronExpressionParser.parse(cronExpression);
        this.dateTimeTemplate = new DateTimeTemplate(Date.from(date.toInstant()), DateTimeZone.forTimeZone(TimeZone.getTimeZone(zoneId)));
    }

    @Override
    protected void incrementDate() {
        while (cronExpression.next(dateTimeTemplate)) {
            DateTime dateTime = dateTimeTemplate.toDateTime();
            if (dateTime != null) {
                date = dateTime.toDate().toInstant().atZone(zoneId);
                return;
            }
        }
    }
}
