/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import ru.mail.jira.plugins.myteam.controller.validation.ChatValidator;

@Documented
@Constraint(validatedBy = ChatValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatValidation {
  String message() default "One of VK Team chats does not exist";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
