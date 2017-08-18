package ru.mail.jira.plugins.calendar.service.recurrent;

import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomDaysOfWeekRecurrenceStrategy extends AbstractDaysOfWeekRecurrenceStrategy {
    @Override
    protected Set<DayOfWeek> getDaysOfWeek(Event event) {
        return Arrays
            .stream(event.getRecurrenceExpression().split(","))
            .map(DayOfWeek::valueOf)
            .collect(Collectors.toSet());
    }

    @Override
    public void validateDto(CustomEventDto customEventDto) {
        //todo
    }
}
