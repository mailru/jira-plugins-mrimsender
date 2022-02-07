/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  Map<String, IssueCreationSettingsDto> getChatSettingsCache();

  IssueCreationSettingsDto getSettings(int id);

  Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId);

  IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings);

  IssueCreationSettingsDto addDefaultSettings(String chatId);

  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings);
}
