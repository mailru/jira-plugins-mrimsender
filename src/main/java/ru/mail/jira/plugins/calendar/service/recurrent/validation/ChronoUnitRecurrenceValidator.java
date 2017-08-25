package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;

public class ChronoUnitRecurrenceValidator extends PeriodAwareRecurrenceValidator {
    @Override
    public void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        validatePeriod(i18nResolver, customEventDto);

        customEventDto.setRecurrenceExpression(null);
    }
}
