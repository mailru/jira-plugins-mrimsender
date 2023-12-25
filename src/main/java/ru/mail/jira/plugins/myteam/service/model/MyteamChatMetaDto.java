/* (C)2023 */
package ru.mail.jira.plugins.myteam.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor(staticName = "of")
@ToString
public class MyteamChatMetaDto {

  private final int id;
  @NotNull private final String chatId;
  @NotNull private final String issueKey;
}
