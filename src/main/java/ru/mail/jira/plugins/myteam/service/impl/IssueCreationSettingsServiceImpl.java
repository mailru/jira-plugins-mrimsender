/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.jira.exception.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettingsEntity;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Service
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;

  public IssueCreationSettingsServiceImpl(
      IssueCreationSettingsRepository issueCreationSettingsRepository) {
    this.issueCreationSettingsRepository = issueCreationSettingsRepository;
  }

  @Override
  public List<IssueCreationSettingsDto> getAllSettings() {
    return issueCreationSettingsRepository.findAll().stream()
        .map(IssueCreationSettingsDto::new)
        .collect(Collectors.toList());
  }

  @Override
  public IssueCreationSettingsDto getSettings(int id) throws NotFoundException {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.get(id));
  }

  @Override
  public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
    return issueCreationSettingsRepository
        .getSettingsByChatId(chatId)
        .map(IssueCreationSettingsDto::new);
  }

  @Override
  public IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings) {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));
  }

  @Override
  public IssueCreationSettingsDto addDefaultSettings(String chatId) {
    IssueCreationSettingsDto settings =
        IssueCreationSettingsDto.builder()
            .chatId(chatId)
            .enabled(false)
            .tag("task")
            .projectKey("")
            .issueTypeId("")
            .labels(new ArrayList<>())
            .build(); // TODO FIX NULLABLE
    @NotNull IssueCreationSettingsEntity res = issueCreationSettingsRepository.create(settings);
    return new IssueCreationSettingsDto(res);
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.update(id, settings));
  }
}
