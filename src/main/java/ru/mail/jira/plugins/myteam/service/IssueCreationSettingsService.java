/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.SettingsTagAlreadyExistsException;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  IssueCreationSettingsDto getSettings(int id);

  @Nullable
  IssueCreationSettingsDto getSettings(String chatId, String tag);

  List<IssueCreationSettingsDto> getSettingsByProjectId(long projectId);

  List<IssueCreationSettingsDto> getSettingsByChatId(String chatId);

  IssueCreationSettingsDto createSettings(IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException;

  boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings);

  boolean hasChatSettings(String chatId, String tag);

  IssueCreationSettingsDto getSettingsById(int id);

  void deleteSettings(int id);
}
