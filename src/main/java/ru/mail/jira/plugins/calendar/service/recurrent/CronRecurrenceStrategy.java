package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.scheduling.support.CronSequenceGenerator;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.sql.Date;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class CronRecurrenceStrategy extends AbstractRecurrenceStrategy {
    @Override
    public List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, EventContext eventContext) {
        ZoneId zoneId = eventContext.getZoneId();

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
                result.add(buildEvent(event, number, startDate, endDate, eventContext));
            }

            number++;
            startDate = cronSequenceGenerator.next(Date.from(startDate.toInstant())).toInstant().atZone(zoneId);
            if (duration != null) {
                endDate = startDate.plus(duration, ChronoUnit.MILLIS);
            }

            if (isLimitExceeded(result.size())) {
                break;
            }
        }

        return result;
    }

    @Override
    public void validate(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        try {
            new CronSequenceGenerator(customEventDto.getRecurrenceExpression()); //no way to validate otherwise in this version of spring
        } catch (IllegalArgumentException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.invalidCronExpression", e.getMessage()), "recurrenceExpression");
        }

        customEventDto.setRecurrencePeriod(null);
    }
}
