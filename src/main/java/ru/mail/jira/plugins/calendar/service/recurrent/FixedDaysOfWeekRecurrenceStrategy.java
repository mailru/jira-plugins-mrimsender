package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.sal.api.message.I18nResolver;
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
    public void validate(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        validatePeriod(i18nResolver, customEventDto);

        customEventDto.setRecurrenceExpression(null);
    }
}
