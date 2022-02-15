/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  IssueCreationSettingsDto getSettings(int id);

  @Nullable
  IssueCreationSettingsDto getSettings(String chatId, String tag);

  Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId);

  IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings);

  IssueCreationSettingsDto addDefaultSettings(String chatId);

  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings);

  boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings);

  boolean hasChatSettings(String chatId, String tag);
}
