/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.exceptions;

import com.atlassian.jira.util.ErrorCollection;
import java.util.stream.Collectors;
import lombok.Getter;

public class IssueCreationValidationException extends Exception {
  @Getter private final ErrorCollection errors;

  public IssueCreationValidationException(String message, ErrorCollection errors) {

    super(
        String.format(
            "%s\n\n%s",
            message,
            errors.getErrors().keySet().stream()
                .map(e -> String.format("%s: %s", e, errors.getErrors().get(e)))
                .collect(Collectors.joining("\n"))));
    this.errors = errors;
  }
}
