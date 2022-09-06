/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

public class Const {

  public static final String CHAT_COMMAND_PREFIX = "/";
  public static final String ISSUE_CREATION_BY_REPLY_PREFIX = "#";
  public static final String DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE =
      "По вашему обращению была создана задача: {{issueKey}}";
  public static final String DEFAULT_ISSUE_SUMMARY_TEMPLATE = "Обращение от {{author}}";
  public static final String DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE = "{quote} {{message}} {quote}";
}
