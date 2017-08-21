package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.time.ZonedDateTime;
import java.util.List;

public interface RecurrenceStrategy {
    List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, EventContext eventContext);

    void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto);
}
