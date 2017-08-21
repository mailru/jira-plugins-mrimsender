package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ChronoUnitRecurrenceStrategy extends AbstractRecurrenceStrategy {
    private final ChronoUnit chronoUnit;

    public ChronoUnitRecurrenceStrategy(ChronoUnit chronoUnit) {
        this.chronoUnit = chronoUnit;
    }

    @Override
    public List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, EventContext eventContext) {
        ZoneId zoneId = eventContext.getZoneId();

        ZonedDateTime startDate = event.getStartDate().toInstant().atZone(zoneId);
        ZonedDateTime endDate = null;
        if (event.getEndDate() != null) {
            endDate = event.getEndDate().toInstant().atZone(zoneId);
        }

        Integer recurrenceCount = event.getRecurrenceCount();
        ZonedDateTime recurrenceEndDate = null;
        if (event.getRecurrenceEndDate() != null) {
            recurrenceEndDate = event.getRecurrenceEndDate().toInstant().atZone(zoneId);
        }

        int period = event.getRecurrencePeriod();
        List<EventDto> result = new ArrayList<>();
        int number = 0;

        while (startDate.isBefore(until) && isBeforeEnd(startDate, recurrenceEndDate) && isCountOk(number, recurrenceCount)) {
            if (startDate.isAfter(since) || endDate != null && endDate.isAfter(since)) {
                result.add(buildEvent(event, number, startDate, endDate, eventContext));
            }

            number++;
            startDate = startDate.plus(period, chronoUnit);
            if (endDate != null) {
                endDate = startDate.plus(period, chronoUnit);
            }

            if (isLimitExceeded(result.size())) {
                break;
            }
        }

        return result;
    }

    @Override
    public void validate(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        validatePeriod(i18nResolver, customEventDto);

        customEventDto.setRecurrenceExpression(null);
    }
}
