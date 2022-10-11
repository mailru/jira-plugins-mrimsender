/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ConditionalValidations {
  ConditionalValidation[] value();
}
