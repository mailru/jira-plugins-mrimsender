/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.SettingsTagAlreadyExistsException;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  @Nullable
  IssueCreationSettingsDto getSettings(int id);

  @Nullable
  IssueCreationSettingsDto getSettingsFromCache(String chatId, String tag);

  List<IssueCreationSettingsDto> getSettingsByProjectId(long projectId);

  List<IssueCreationSettingsDto> getSettingsByChatId(String chatId);

  @Nullable
  IssueCreationSettingsDto createSettings(@NotNull IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  @Nullable
  IssueCreationSettingsDto updateSettings(int id, @NotNull IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings);

  boolean hasChatSettings(String chatId, String tag);

  @Nullable
  IssueCreationSettingsDto getSettingsById(int id);

  void deleteSettings(int id);
}
