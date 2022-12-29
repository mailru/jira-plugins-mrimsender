/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.OneNotFalseValidator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Constraint(validatedBy = {OneNotFalseValidator.class})
public @interface OneNotFalseValidation {
  String message() default "Оne of the fields must be true.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] fields();

  String errorField();
}
