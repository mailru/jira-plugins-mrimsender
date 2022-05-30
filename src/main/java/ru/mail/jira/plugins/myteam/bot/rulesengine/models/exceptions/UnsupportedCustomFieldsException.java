/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions;

import com.atlassian.jira.issue.fields.Field;
import java.util.List;
import lombok.Getter;

public class UnsupportedCustomFieldsException extends Exception {

  @Getter private final List<Field> requiredCustomFields;

  public UnsupportedCustomFieldsException(String message, List<Field> requiredCustomFields) {
    super(message);
    this.requiredCustomFields = requiredCustomFields;
  }
}
