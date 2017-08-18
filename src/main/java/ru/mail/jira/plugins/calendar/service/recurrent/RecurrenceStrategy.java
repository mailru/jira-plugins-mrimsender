package ru.mail.jira.plugins.calendar.service.recurrent;

import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public interface RecurrenceStrategy {
    List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, ZoneId zoneId);

    void validateDto(CustomEventDto customEventDto);
}
