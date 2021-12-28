/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.exceptions;

import com.atlassian.jira.util.ErrorCollection;
import lombok.Getter;

public class IssueCreationValidationException extends Exception {
  @Getter private final ErrorCollection errors;

  public IssueCreationValidationException(String message, ErrorCollection errors) {
    super(message);
    this.errors = errors;
  }
}
