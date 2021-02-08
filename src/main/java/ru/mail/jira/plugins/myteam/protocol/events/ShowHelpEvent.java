/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

@Getter
public class ShowHelpEvent {
  private final String chatId;
  private final String userId;
  private final ChatType chatType;

  public ShowHelpEvent(ChatMessageEvent chatMessageEvent) {
    this.chatId = chatMessageEvent.getChatId();
    this.userId = chatMessageEvent.getUserId();
    this.chatType = chatMessageEvent.getChatType();
  }
}
