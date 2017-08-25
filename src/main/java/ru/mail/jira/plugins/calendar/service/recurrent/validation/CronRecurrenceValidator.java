package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import org.quartz.CronExpression;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.text.ParseException;

public class CronRecurrenceValidator implements RecurrenceValidator {
    @Override
    public void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        try {
            CronExpression.validateExpression(customEventDto.getRecurrenceExpression()); //no way to validate otherwise in this version of spring
        } catch (ParseException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurrence.error.invalidCronExpression", e.getMessage()), "recurrenceExpression");
        }

        customEventDto.setRecurrencePeriod(null);
    }
}
