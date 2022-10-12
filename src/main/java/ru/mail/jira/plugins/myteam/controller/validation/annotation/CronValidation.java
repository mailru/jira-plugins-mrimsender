/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.controller.validation.CronValidator;

@Documented
@Constraint(validatedBy = CronValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CronValidation {
  String message() default "Cron expression is not valid.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
