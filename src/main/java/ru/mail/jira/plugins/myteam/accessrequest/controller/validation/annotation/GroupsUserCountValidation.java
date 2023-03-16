/* (C)2023 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.GroupsUserCountValidator;

@Documented
@Constraint(validatedBy = GroupsUserCountValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GroupsUserCountValidation {
  String message() default "The number of users is too large.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
