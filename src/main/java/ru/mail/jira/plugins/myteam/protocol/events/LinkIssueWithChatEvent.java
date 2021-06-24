/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class LinkIssueWithChatEvent {
  private final String chatId;
  private final String userId;
  private final String issueKey;

  public LinkIssueWithChatEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    this.issueKey =
        StringUtils.substringAfter(chatMessageEvent.getMessage().trim().toLowerCase(), "/link")
            .trim();
  }
}
