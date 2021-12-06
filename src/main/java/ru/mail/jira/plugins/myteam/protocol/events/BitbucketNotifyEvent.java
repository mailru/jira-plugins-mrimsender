/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol.events;

import java.util.List;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

@Getter
public class BitbucketNotifyEvent extends MyteamEvent {
  private final String message;
  private final List<List<InlineKeyboardMarkupButton>> buttons;

  public BitbucketNotifyEvent(
      String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) {
    this.chatId = chatId;
    this.message = message;
    this.buttons = buttons;
  }
}
