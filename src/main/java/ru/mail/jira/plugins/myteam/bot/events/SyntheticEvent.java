/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.events;

import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

public class SyntheticEvent extends MyteamEvent {

  public SyntheticEvent(String chatId, String userId, @Nullable ChatType chatType) {
    super(chatId, userId, chatType);
  }
}
