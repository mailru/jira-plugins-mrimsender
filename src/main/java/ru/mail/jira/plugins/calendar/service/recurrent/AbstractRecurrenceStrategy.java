package ru.mail.jira.plugins.calendar.service.recurrent;

import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public abstract class AbstractRecurrenceStrategy implements RecurrenceStrategy {
    protected EventDto buildEvent(Event originalEvent, int number, ZonedDateTime startDate, ZonedDateTime endDate) {
        EventDto result = new EventDto();
        result.setId(originalEvent.getID() + "-" + number);
        result.setTitle(originalEvent.getTitle());
        result.setAllDay(originalEvent.isAllDay());
        result.setStart(DateTimeFormatter.ISO_INSTANT.format(startDate));
        result.setEnd(endDate != null ? DateTimeFormatter.ISO_ZONED_DATE_TIME.format(endDate) : null);
        return result;
    }

    protected boolean isBeforeEnd(ZonedDateTime time, ZonedDateTime endTime) {
        return endTime == null || time.isBefore(endTime);
    }

    protected boolean isCountOk(int number, Integer recurrenceCount) {
        return recurrenceCount == null || number <= recurrenceCount;
    }
}
