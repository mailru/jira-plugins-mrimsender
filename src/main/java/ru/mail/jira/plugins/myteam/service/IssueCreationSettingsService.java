/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import java.util.List;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  IssueCreationSettingsDto getSettings(int id);

  IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings);

  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings);
}
