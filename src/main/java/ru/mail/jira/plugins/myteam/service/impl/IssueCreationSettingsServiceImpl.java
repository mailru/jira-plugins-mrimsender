/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Slf4j
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
    log.error(
        MessageFormatter.formLogMessage(
            "getSettings(String chatId, String tag)",
            "Start",
            ImmutableMap.of("chatId", chatId, "tag", tag)));

    log.error(
        MessageFormatter.formLogMessage(
            "getSettings(String chatId, String tag)",
            "Issue Settings Cache Stats",
            ImmutableMap.of(
                "size",
                String.valueOf(issueSettingsCache.getKeys().size()),
                "keys",
                String.join(",", issueSettingsCache.getKeys()))));
    IssueCreationSettingsDto settings =
        issueSettingsCache
            .get(chatId)
            .filter(settingsDto -> tag.equals(settingsDto.getTag()))
            .orElse(null);

    if (settings == null) {
      log.error(
          MessageFormatter.formLogMessage(
              "getSettings(String chatId, String tag)",
              "issueCreationSettingsService.getSettings returned null"));
    } else {
      log.error(
          MessageFormatter.formLogMessage(
              "getSettings(String chatId, String tag)",
              "Result",
              ImmutableMap.of(
                  "id",
                  String.valueOf(settings.getId()),
                  "enabled",
                  String.valueOf(settings.getEnabled()),
                  "tag",
                  settings.getTag(),
                  "project",
                  settings.getProjectKey(),
                  "issueType",
                  settings.getIssueTypeId())));
    }
    return settings;
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
        IssueCreationSettingsDto.builder().chatId(chatId).enabled(false).tag("task").build();

    log.error(
        MessageFormatter.formLogMessage(
            "getSettings(String chatId, String tag)",
            "Before create. Cache stats",
            ImmutableMap.of(
                "size",
                String.valueOf(issueSettingsCache.getKeys().size()),
                "keys",
                String.join(",", issueSettingsCache.getKeys()))));

    issueCreationSettingsRepository.create(settings);
    issueSettingsCache.remove(chatId);

    IssueCreationSettingsDto result = issueSettingsCache.get(chatId).orElse(null); // NotNull

    log.error(
        MessageFormatter.formLogMessage(
            "addDefaultSettings(String chatId)",
            "Result. Cache stats",
            ImmutableMap.of(
                "size",
                String.valueOf(issueSettingsCache.getKeys().size()),
                "keys",
                String.join(",", issueSettingsCache.getKeys()))));
    return result;
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {

    log.error(
        MessageFormatter.formLogMessage(
            "updateSettings(int id, IssueCreationSettingsDto settings)",
            "Before create. Cache stats",
            ImmutableMap.of(
                "size",
                String.valueOf(issueSettingsCache.getKeys().size()),
                "keys",
                String.join(",", issueSettingsCache.getKeys()))));
    issueCreationSettingsRepository.update(id, settings);
    issueSettingsCache.remove(settings.getChatId());

    IssueCreationSettingsDto result =
        issueSettingsCache.get(settings.getChatId()).orElse(null); // NotNull

    log.error(
        MessageFormatter.formLogMessage(
            "updateSettings(int id, IssueCreationSettingsDto settings)",
            "Result. Cache stats",
            ImmutableMap.of(
                "size",
                String.valueOf(issueSettingsCache.getKeys().size()),
                "keys",
                String.join(",", issueSettingsCache.getKeys()))));
    return result;
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
    log.error(
        MessageFormatter.formLogMessage(
            "hasChatSettings(String chatId, String tag)",
            "Start",
            ImmutableMap.of("chatId", chatId, "tag", tag)));

    boolean result =
        issueSettingsCache
            .get(chatId)
            .filter(settingsDto -> tag.equals(settingsDto.getTag()))
            .isPresent(); // NotNull

    log.error(
        MessageFormatter.formLogMessage(
            "hasChatSettings(String chatId, String tag)",
            "Result",
            ImmutableMap.of("isPresent", String.valueOf(result), "tag", tag, "chatId", chatId)));
    return result;
  }
}
