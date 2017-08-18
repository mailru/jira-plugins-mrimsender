package ru.mail.jira.plugins.calendar.service.recurrent;

import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;

import java.time.DayOfWeek;
import java.util.Set;

public class FixedDaysOfWeekRecurrenceStrategy extends AbstractDaysOfWeekRecurrenceStrategy {
    private final Set<DayOfWeek> daysOfWeeks;

    public FixedDaysOfWeekRecurrenceStrategy(Set<DayOfWeek> daysOfWeeks) {
        this.daysOfWeeks = daysOfWeeks;
    }

    @Override
    protected Set<DayOfWeek> getDaysOfWeek(Event event) {
        return daysOfWeeks;
    }

    @Override
    public void validateDto(CustomEventDto customEventDto) {
        //todo
    }
}
