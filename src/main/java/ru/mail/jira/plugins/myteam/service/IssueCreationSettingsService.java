/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.SettingsTagAlreadyExistsException;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;

public interface IssueCreationSettingsService {

  @Nullable
  IssueCreationSettingsDto getSettings(int id);

  @Nullable
  IssueCreationSettingsDto getSettingsFromCache(String chatId, String tag);

  List<IssueCreationSettingsDto> getSettingsByProjectKey(String projectKey);

  List<IssueCreationSettingsDto> getSettingsByProjectId(final long projectId);

  List<IssueCreationSettingsDto> getSettingsByChatId(String chatId);

  @Nullable
  IssueCreationSettingsDto createSettings(IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  @Nullable
  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings);

  boolean hasChatSettings(String chatId, String tag);

  @Nullable
  IssueCreationSettingsDto getSettingsById(int id);

  void deleteSettings(int id);
}
