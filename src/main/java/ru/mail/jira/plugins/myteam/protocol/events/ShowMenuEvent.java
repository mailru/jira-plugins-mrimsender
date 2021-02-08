/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class ShowMenuEvent {
  private final String userId;
  private final String chatId;

  public ShowMenuEvent(ChatMessageEvent chatMessageEvent) {
    this.userId = chatMessageEvent.getUserId();
    this.chatId = chatMessageEvent.getChatId();
  }

  public ShowMenuEvent(ButtonClickEvent buttonClickEvent) {
    this.userId = buttonClickEvent.getUserId();
    this.chatId = buttonClickEvent.getChatId();
  }
}
