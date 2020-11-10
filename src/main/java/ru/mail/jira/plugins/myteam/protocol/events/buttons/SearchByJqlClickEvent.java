/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

@Getter
public class SearchByJqlClickEvent {
  private final String chatId;
  private final String userId;
  private final String queryId;

  public SearchByJqlClickEvent(ButtonClickEvent chatButtonClickEvent) {
    this.queryId = chatButtonClickEvent.getQueryId();
    this.userId = chatButtonClickEvent.getUserId();
    this.chatId = chatButtonClickEvent.getChatId();
  }
}
