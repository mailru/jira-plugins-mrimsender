/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

/**
 * As a result of this event handled user should get next formatted message
 *
 * <p>---------------------------------------------------- 1. KEY-123 Issue summary comes here ...
 * 2. KEY-124 Issue summary comes here ...
 *
 * <p>n. KEY-612 Issue summary comes here ...
 *
 * <p>| go next page button | go prev page button |
 * ----------------------------------------------------
 */
@Getter
public class SearchIssuesClickEvent {
  private final String jqlClause;
  private final String chatId;
  private final String userId;
  private final String queryId;

  public SearchIssuesClickEvent(ButtonClickEvent chatButtonClickEvent, String jqlClause) {
    this.chatId = chatButtonClickEvent.getChatId();
    this.userId = chatButtonClickEvent.getUserId();
    this.queryId = chatButtonClickEvent.getQueryId();
    this.jqlClause = jqlClause;
  }
}
