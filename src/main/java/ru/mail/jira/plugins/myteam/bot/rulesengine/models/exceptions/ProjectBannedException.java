/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions;

public class ProjectBannedException extends Exception {
  public ProjectBannedException(String message) {
    super(message);
  }
}
