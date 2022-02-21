/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class MyteamEvent {
  protected String chatId;
  protected String userId;
  @Nullable protected ChatType chatType;
}
