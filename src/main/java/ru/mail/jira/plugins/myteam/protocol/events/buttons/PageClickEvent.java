/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

public interface PageClickEvent {
  String getChatId();

  long getMsgId();

  String getUserId();

  int getCurrentPage();

  String getQueryId();
}
