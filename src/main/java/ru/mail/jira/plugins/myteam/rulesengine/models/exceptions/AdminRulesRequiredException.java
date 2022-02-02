/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.models.exceptions;

public class AdminRulesRequiredException extends Exception {
  public AdminRulesRequiredException() {
    super("Command is available only for chat admin users");
  }
}
