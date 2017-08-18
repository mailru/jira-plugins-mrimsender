package ru.mail.jira.plugins.calendar.service.recurrent;

import org.springframework.scheduling.support.CronSequenceGenerator;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class CronRecurrenceStrategy extends AbstractRecurrenceStrategy {
    @Override
    public List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, ZoneId zoneId) {
        CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(event.getRecurrenceExpression(), TimeZone.getTimeZone(zoneId));

        ZonedDateTime startDate = event.getStartDate().toInstant().atZone(zoneId);

        Long duration = null;
        ZonedDateTime endDate = null;
        if (event.getEndDate() != null) {
            duration = event.getEndDate().getTime() - event.getStartDate().getTime();
            endDate = event.getEndDate().toInstant().atZone(zoneId); //get initial end date from event
        }

        Integer recurrenceCount = event.getRecurrenceCount();
        ZonedDateTime recurrenceEndDate = null;
        if (event.getRecurrenceEndDate() != null) {
            recurrenceEndDate = event.getRecurrenceEndDate().toInstant().atZone(zoneId);
        }

        List<EventDto> result = new ArrayList<>();
        int number = 0;

        while (startDate.isBefore(until) && isBeforeEnd(startDate, recurrenceEndDate) && isCountOk(number, recurrenceCount)) {
            if (startDate.isAfter(since) || endDate != null && endDate.isAfter(since)) {
                result.add(buildEvent(event, number, startDate, endDate));
            }

            number++;
            startDate = cronSequenceGenerator.next(Date.from(startDate.toInstant())).toInstant().atZone(zoneId);
            if (duration != null) {
                endDate = startDate.plus(duration, ChronoUnit.MILLIS);
            }
        }

        return result;
    }

    @Override
    public void validateDto(CustomEventDto customEventDto) {
        //todo
    }
}
