/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.events;

import lombok.*;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
public abstract class MyteamEvent {
  protected String chatId;
  protected String userId;
  @Nullable protected ChatType chatType;
}
