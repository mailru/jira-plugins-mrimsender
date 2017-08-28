package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CustomDaysOfWeekRecurrenceValidator extends PeriodAwareRecurrenceValidator {
    @Override
    public void validateDto(I18nResolver i18nResolver, CustomEventDto event) {
        validatePeriod(i18nResolver, event);

        List<DayOfWeek> collect = Arrays
            .stream(event.getRecurrenceExpression().split(","))
            .map(StringUtils::trimToNull)
            .filter(Objects::nonNull)
            .map(value -> tryParseDayOfWeek(i18nResolver, value))
            .distinct()
            .sorted(Comparator.comparing(DayOfWeek::getValue))
            .collect(Collectors.toList());

        if (collect.size() == 0) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.atLeastOneDayOfWeek"), "recurrenceDaysOfWeek");
        }

        event.setRecurrenceExpression(collect.stream().map(DayOfWeek::toString).collect(Collectors.joining(",")));
    }

    private static DayOfWeek tryParseDayOfWeek(I18nResolver i18nResolver, String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.unknownDayOfWeek", value), "recurrenceDaysOfWeek");
        }
    }
}
