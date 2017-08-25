package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

public abstract class PeriodAwareRecurrenceValidator implements RecurrenceValidator {
    protected void validatePeriod(I18nResolver i18nResolver, CustomEventDto eventDto) {
        if (eventDto.getRecurrencePeriod() == null) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.period")), "recurrencePeriod");
        }

        if (eventDto.getRecurrencePeriod() <= 0) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.incorrectPeriod"), "recurrencePeriod");
        }
    }
}
