/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.events;

import java.util.List;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

@Getter
public class JiraNotifyEvent extends MyteamEvent {
  @Nullable private final String message;
  @Nullable private final List<List<InlineKeyboardMarkupButton>> buttons;

  public JiraNotifyEvent(
      String chatId,
      @Nullable String message,
      @Nullable List<List<InlineKeyboardMarkupButton>> buttons) {
    this.chatId = chatId;
    this.message = message;
    this.buttons = buttons;
  }
}
