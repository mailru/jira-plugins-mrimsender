/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions;

import com.atlassian.jira.util.ErrorCollection;

public class IssueCreationValidationException extends ErrorCollectionException {

  public IssueCreationValidationException(String message, ErrorCollection errors) {
    super(message, errors);
  }
}
