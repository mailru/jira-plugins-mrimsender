package ru.mail.jira.plugins.calendar.service.recurrent;

import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractDaysOfWeekRecurrenceStrategy extends AbstractRecurrenceStrategy {
    @Override
    public List<EventDto> getEventsInRange(Event event, ZonedDateTime since, ZonedDateTime until, ZoneId zoneId) {
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

        List<EventDto> result = new ArrayList<>();

        Set<DayOfWeek> daysOfWeek = getDaysOfWeek(event);
        int period = event.getRecurrencePeriod() - 1;
        int number = 0;

        while (startDate.isBefore(until) && isBeforeEnd(startDate, recurrenceEndDate) && isCountOk(number, recurrenceCount)) {
            if (daysOfWeek.contains(startDate.getDayOfWeek()) || number == 0) {
                if (startDate.isAfter(since) || endDate != null && endDate.isAfter(since)) {
                    result.add(buildEvent(event, number, startDate, endDate));
                }

                number++;
            }

            int plusDays = 1;

            if (startDate.getDayOfWeek() == DayOfWeek.SUNDAY){
                plusDays = period*7 + 1;
            }
            startDate = startDate.plusDays(plusDays);
            if (endDate != null) {
                endDate = endDate.plusDays(plusDays);
            }
        }

        return result;
    }

    protected abstract Set<DayOfWeek> getDaysOfWeek(Event event);
}
