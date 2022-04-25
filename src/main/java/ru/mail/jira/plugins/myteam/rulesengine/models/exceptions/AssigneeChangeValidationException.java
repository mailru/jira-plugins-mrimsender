/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.models.exceptions;

import com.atlassian.jira.util.ErrorCollection;

public class AssigneeChangeValidationException extends ErrorCollectionException {
  public AssigneeChangeValidationException(String message, ErrorCollection errors) {
    super(message, errors);
  }
}
