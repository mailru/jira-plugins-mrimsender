package ru.mail.jira.plugins.myteam.service;

import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;

import java.util.List;

public interface IssueCreationSettingsService {

  List<IssueCreationSettingsDto> getAllSettings();

  IssueCreationSettingsDto getSettings(int id);

  IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings);

  IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings);

}
