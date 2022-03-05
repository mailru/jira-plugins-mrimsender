/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE;
import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_SUMMARY_TEMPLATE;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Service
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private static final String CACHE_NAME =
      IssueCreationSettingsServiceImpl.class.getName() + ".issueSettingsCache";

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;

  private final Cache<String, Optional<IssueCreationSettingsDto>> issueSettingsCache;

  public IssueCreationSettingsServiceImpl(
      IssueCreationSettingsRepository issueCreationSettingsRepository,
      @ComponentImport CacheManager cacheManager) {
    this.issueCreationSettingsRepository = issueCreationSettingsRepository;

    issueSettingsCache =
        cacheManager.getCache(
            CACHE_NAME,
            this::getSettingsByChatId,
            new CacheSettingsBuilder().remote().replicateViaInvalidation().build());
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
  @Nullable
  public IssueCreationSettingsDto getSettings(String chatId, String tag) {
    return issueSettingsCache
        .get(chatId)
        .filter(settingsDto -> tag.equals(settingsDto.getTag()))
        .orElse(null);
  }

  @NotNull
  @Override
  public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
    return issueCreationSettingsRepository
        .getSettingsByChatId(chatId)
        .map(IssueCreationSettingsDto::new);
  }

  @Override
  public IssueCreationSettingsDto addDefaultSettings(String chatId) {
    IssueCreationSettingsDto settings =
        IssueCreationSettingsDto.builder()
            .chatId(chatId)
            .enabled(false)
            .tag("task")
            .creationSuccessTemplate(DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE)
            .issueSummaryTemplate(DEFAULT_ISSUE_SUMMARY_TEMPLATE)
            .build();

    issueCreationSettingsRepository.create(settings);
    issueSettingsCache.remove(chatId);

    return issueSettingsCache.get(chatId).orElse(null); // NotNull
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
    issueCreationSettingsRepository.update(id, settings);
    issueSettingsCache.remove(settings.getChatId());

    return issueSettingsCache.get(settings.getChatId()).orElse(null); // NotNull
  }

  @Override
  public boolean hasRequiredFields(@Nullable IssueCreationSettingsDto settings) {
    return settings != null
        && settings.getEnabled()
        && settings.getProjectKey() != null
        && settings.getIssueTypeId() != null;
  }

  @Override
  public boolean hasChatSettings(String chatId, String tag) {
    return issueSettingsCache
        .get(chatId)
        .filter(settingsDto -> tag.equals(settingsDto.getTag()))
        .isPresent(); // NotNull
  }
}
