/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation;

import static org.springframework.util.StringUtils.isEmpty;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.beanutils.BeanUtils;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.validation.annotation.ConditionalValidation;

public class ConditionalValidator implements ConstraintValidator<ConditionalValidation, Object> {
  @Nullable private String conditionalProperty;
  @Nullable private String[] requiredProperties;
  @Nullable private String message;
  @Nullable private String[] values;

  @Override
  public void initialize(ConditionalValidation constraint) {
    conditionalProperty = constraint.conditionalProperty();
    requiredProperties = constraint.requiredProperties();
    message = constraint.message();
    values = constraint.values();
  }

  @Override
  public boolean isValid(Object object, ConstraintValidatorContext context) {
    try {
      Object conditionalPropertyValue = BeanUtils.getProperty(object, conditionalProperty);
      if (doConditionalValidation(conditionalPropertyValue)) {
        return validateRequiredProperties(object, context);
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
      return false;
    }
    return true;
  }

  private boolean validateRequiredProperties(Object object, ConstraintValidatorContext context)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    boolean isValid = true;
    if (requiredProperties != null) {
      for (String property : requiredProperties) {
        Object requiredValue = BeanUtils.getProperty(object, property);
        boolean isPresent = requiredValue != null && !isEmpty(requiredValue);
        if (!isPresent) {
          isValid = false;
          context.disableDefaultConstraintViolation();
          context
              .buildConstraintViolationWithTemplate(message)
              .addPropertyNode(property)
              .addConstraintViolation();
        }
      }
    }
    return isValid;
  }

  private boolean doConditionalValidation(Object actualValue) {
    return Arrays.asList(values).contains(actualValue);
  }
}
