/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions;

import com.atlassian.jira.util.ErrorCollection;

public class IssueTransitionException extends ErrorCollectionException {
  public IssueTransitionException(String message, ErrorCollection errors) {
    super(message, errors);
  }
}
