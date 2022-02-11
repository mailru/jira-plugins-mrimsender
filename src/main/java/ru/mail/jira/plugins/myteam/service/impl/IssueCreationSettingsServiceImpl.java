/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.util.map.CacheObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Service
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;

  private final Cache<String, CacheObject<Map<String, IssueCreationSettingsDto>>>
      issueSettingsCache; // Settings cache. Where key is "{chatId}-{tag}".
  // Example: 74175@chat.agent-#task

  public IssueCreationSettingsServiceImpl(
      IssueCreationSettingsRepository issueCreationSettingsRepository,
      @ComponentImport CacheManager cacheManager) {
    this.issueCreationSettingsRepository = issueCreationSettingsRepository;

    issueSettingsCache =
        cacheManager.getCache(
            getCacheKey(),
            new IssueSettingsCacheLoader(),
            new CacheSettingsBuilder().remote().unflushable().build());
  }

  @Override
  public List<IssueCreationSettingsDto> getAllSettings() {
    return issueCreationSettingsRepository.findAll().stream()
        .map(IssueCreationSettingsDto::new)
        .collect(Collectors.toList());
  }

  @Override
  public Map<String, IssueCreationSettingsDto> getChatSettingsCache() {
    return getSettingsCache().orElse(ImmutableMap.of());
  }

  @Override
  public IssueCreationSettingsDto getSettings(int id) throws NotFoundException {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.get(id));
  }

  @Override
  @Nullable
  public IssueCreationSettingsDto getSettings(String chatId, String tag) {
    if (!hasChatSettings(chatId, tag)) return null;

    return getSettingsCache().map(s -> s.get(getSettingsCacheKey(chatId, tag))).orElse(null);
  }

  @Override
  public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
    return issueCreationSettingsRepository
        .getSettingsByChatId(chatId)
        .map(IssueCreationSettingsDto::new);
  }

  @Override
  public IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings) {
    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));

    getSettingsCache().ifPresent(s -> s.put(getSettingsCacheKey(result), result));
    return result;
  }

  @Override
  public IssueCreationSettingsDto addDefaultSettings(String chatId) {
    IssueCreationSettingsDto settings =
        IssueCreationSettingsDto.builder().chatId(chatId).enabled(false).tag("task").build();

    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));

    getSettingsCache().ifPresent(s -> s.put(getSettingsCacheKey(result), result));
    return result;
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
    IssueCreationSettingsDto oldSettings = getSettings(id);

    getSettingsCache().ifPresent(s -> s.remove(getSettingsCacheKey(oldSettings)));

    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.update(id, settings));

    getSettingsCache().ifPresent(s -> s.put(getSettingsCacheKey(result), result));
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
    return getSettingsCache()
        .map(settings -> settings.containsKey(getSettingsCacheKey(chatId, tag)))
        .orElse(false);
  }

  private Optional<Map<String, IssueCreationSettingsDto>> getSettingsCache() {
    CacheObject<Map<String, IssueCreationSettingsDto>> cache =
        issueSettingsCache.get(getCacheKey());
    return Optional.ofNullable(cache != null ? cache.getValue() : null);
  }

  private String getSettingsCacheKey(IssueCreationSettingsDto settings) {
    return getSettingsCacheKey(settings.getChatId(), settings.getTag());
  }

  private String getSettingsCacheKey(String chatId, String tag) {
    return String.format("%s-%s", chatId, tag);
  }

  private String getCacheKey() {
    return IssueCreationSettingsServiceImpl.class.getName() + ".issueSettingsCache";
  }

  private class IssueSettingsCacheLoader
      implements CacheLoader<String, CacheObject<Map<String, IssueCreationSettingsDto>>> {
    @Override
    public @NotNull CacheObject<Map<String, IssueCreationSettingsDto>> load(
        @Nonnull final String fieldConfigId) {
      Map<String, IssueCreationSettingsDto> issueSettingsCache = new HashMap<>();
      issueCreationSettingsRepository
          .findAll()
          .forEach(
              settings ->
                  issueSettingsCache.put(
                      String.format("%s-%s", settings.getChatId(), settings.getTag()),
                      new IssueCreationSettingsDto(settings)));
      return CacheObject.wrap(issueSettingsCache);
    }
  }
}
