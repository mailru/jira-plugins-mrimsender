package ru.mail.jira.plugins.calendar.service.recurrent.validation;

import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.scheduler.caesium.cron.CaesiumCronExpressionValidator;
import com.atlassian.scheduler.cron.CronSyntaxException;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

public class CronRecurrenceValidator implements RecurrenceValidator {
    private final CaesiumCronExpressionValidator caesiumCronExpressionValidator = new CaesiumCronExpressionValidator();

    @Override
    public void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        try {
            caesiumCronExpressionValidator.validate(customEventDto.getRecurrenceExpression());
        } catch (CronSyntaxException e) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.invalidCronExpression", e.getMessage()), "recurrenceExpression");
        }

        customEventDto.setRecurrencePeriod(null);
    }
}
