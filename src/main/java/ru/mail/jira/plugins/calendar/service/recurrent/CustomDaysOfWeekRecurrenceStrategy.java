package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

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
    public void validate(I18nResolver i18nResolver, CustomEventDto event) {
        validatePeriod(i18nResolver, event);

        Set<DayOfWeek> collect = Arrays
            .stream(event.getRecurrenceExpression().split(","))
            .map(value -> tryParseDayOfWeek(i18nResolver, value))
            .collect(Collectors.toSet());

        if (collect.size() == 0) {
            throw new RestFieldException("At least one day of week should be selected", "daysOfWeek"); //todo
        }

        event.setRecurrenceExpression(collect.stream().map(DayOfWeek::toString).collect(Collectors.joining(",")));
    }

    private static DayOfWeek tryParseDayOfWeek(I18nResolver i18nResolver, String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.unknownRecurrenceType"), "recurrenceType");
        }
    }
}
