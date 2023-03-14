/* (C)2023 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.ProjectRolesProjectRolesValidator;

@Documented
@Constraint(validatedBy = ProjectRolesProjectRolesValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectRolesUserCountValidation {
  String message() default "The number of users is too large.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
