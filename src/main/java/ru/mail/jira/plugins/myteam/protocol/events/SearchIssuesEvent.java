/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

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
 *
 * <p>Instead of SearchIssuesClickEvent class this event published when JqlClause came from user
 * message
 */
@Getter
public class SearchIssuesEvent {
  private final String chatId;
  private final String jqlClause;
  private final String userId;

  public SearchIssuesEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.jqlClause = chatMessageEvent.getMessage().trim();
    this.userId = chatMessageEvent.getUerId();
  }
}
