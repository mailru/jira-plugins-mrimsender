/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation;

import com.atlassian.sal.api.message.I18nResolver;
import java.lang.reflect.InvocationTargetException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.OneNotFalseValidation;
import ru.mail.jira.plugins.myteam.controller.validation.provider.ContextProvider;

@SuppressWarnings("NullAway")
public class OneNotFalseValidator implements ConstraintValidator<OneNotFalseValidation, Object> {
  @Nullable private I18nResolver i18nResolver;
  @Nullable private String[] fields;
  @Nullable private String errorField;

  @Override
  public void initialize(final OneNotFalseValidation constraint) {
    this.i18nResolver = (I18nResolver) ContextProvider.getBean(I18nResolver.class);
    this.fields = constraint.fields();
    this.errorField = constraint.errorField();
  }

  @Override
  public boolean isValid(final Object object, final ConstraintValidatorContext context) {
    if (fields != null) {
      boolean isValid = false;
      for (String field : fields) {
        try {
          String fieldValue = BeanUtils.getProperty(object, field);
          if (Boolean.parseBoolean(fieldValue)) {
            isValid = true;
            break;
          }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
          return false;
        }
      }
      if (!isValid && errorField != null && i18nResolver != null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                i18nResolver.getText(
                    "ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.validation.false"))
            .addPropertyNode(errorField)
            .addConstraintViolation();
      }
      return isValid;
    } else {
      return true;
    }
  }
}
