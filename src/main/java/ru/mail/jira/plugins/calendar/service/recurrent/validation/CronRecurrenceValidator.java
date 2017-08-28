package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.scheduling.support.CronSequenceGenerator;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

public class CronRecurrenceValidator implements RecurrenceValidator {
    @Override
    public void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        try {
            new CronSequenceGenerator(customEventDto.getRecurrenceExpression()); //no way to validate otherwise in this version of spring
        } catch (IllegalArgumentException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.invalidCronExpression", e.getMessage()), "recurrenceExpression");
        }

        customEventDto.setRecurrencePeriod(null);
    }
}
