/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.events;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

@Getter
public class JiraNotifyEvent extends MyteamEvent {
  @NotNull private final String message;
  @Nullable private final List<List<InlineKeyboardMarkupButton>> buttons;

  public JiraNotifyEvent(
      String chatId,
      @NotNull String message,
      @Nullable List<List<InlineKeyboardMarkupButton>> buttons) {
    this.chatId = chatId;
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.buttons = buttons;
  }
}
