/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation;

import com.atlassian.jira.util.I18nHelper;
import com.atlassian.scheduler.SchedulerService;
import io.atlassian.fugue.Option;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.CronValidation;
import ru.mail.jira.plugins.myteam.controller.validation.provider.ContextProvider;

public class CronValidator implements ConstraintValidator<CronValidation, String> {
  @Nullable private I18nHelper i18nHelper;
  @Nullable private SchedulerService schedulerService;

  @Override
  public void initialize(CronValidation constraintAnnotation) {
    this.i18nHelper = (I18nHelper) ContextProvider.getBean(I18nHelper.class);
    this.schedulerService = (SchedulerService) ContextProvider.getBean(SchedulerService.class);
  }

  @Override
  public boolean isValid(String cronString, ConstraintValidatorContext constraintValidatorContext) {
    boolean isValid = true;
    if (StringUtils.isNotBlank(cronString) && i18nHelper != null && schedulerService != null) {
      Option<String> error =
          new com.atlassian.jira.scheduler.cron.CronValidator(i18nHelper, schedulerService)
              .validateCron(cronString);
      if (!error.isEmpty()) {
        isValid = false;
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext
            .buildConstraintViolationWithTemplate(error.get())
            .addConstraintViolation();
      }
    }
    return isValid;
  }
}
