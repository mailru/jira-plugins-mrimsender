package ru.mail.jira.plugins.myteam.service.impl;

import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

import java.util.List;
import java.util.stream.Collectors;

public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;

  public IssueCreationSettingsServiceImpl(IssueCreationSettingsRepository issueCreationSettingsRepository) {
    this.issueCreationSettingsRepository = issueCreationSettingsRepository;
  }

  @Override
  public List<IssueCreationSettingsDto> getAllSettings() {
    return issueCreationSettingsRepository.findAll()
        .stream()
        .map(IssueCreationSettingsDto::new)
        .collect(Collectors.toList());
  }

  @Override
  public IssueCreationSettingsDto getSettings(int id) {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.get(id));
  }

  @Override
  public IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings) {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.update(id, settings));
  }
}
