/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.OneNotNullValidator;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = {OneNotNullValidator.class})
public @interface OneNotNullValidation {
  String message() default "Оne of this fields must be not null.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  String[] fields();

  String errorField();
}
