/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions;

public class IncorrectIssueTypeException extends Exception {
  public IncorrectIssueTypeException(String message) {
    super(message);
  }
}
