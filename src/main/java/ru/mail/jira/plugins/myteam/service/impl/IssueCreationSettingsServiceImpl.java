/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.jira.exception.NotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Service
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;

  private Map<String, IssueCreationSettingsDto>
      issueSettingsCache; // Settings cache. Where key is "{chatId}-{tag}".
  // Example: 74175@chat.agent-#task

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
  public Map<String, IssueCreationSettingsDto> getChatSettingsCache() {
    checkAndFillCache();
    return issueSettingsCache;
  }

  @Override
  public IssueCreationSettingsDto getSettings(int id) throws NotFoundException {
    return new IssueCreationSettingsDto(issueCreationSettingsRepository.get(id));
  }

  @Override
  @Nullable
  public IssueCreationSettingsDto getSettings(String chatId, String tag) {
    if (!hasChatSettings(chatId, tag)) return null;

    return issueSettingsCache.get(getSettingsCacheKey(chatId, tag));
  }

  @Override
  public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
    return issueCreationSettingsRepository
        .getSettingsByChatId(chatId)
        .map(IssueCreationSettingsDto::new);
  }

  @Override
  public IssueCreationSettingsDto addSettings(IssueCreationSettingsDto settings) {
    checkAndFillCache();

    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));
    issueSettingsCache.put(getSettingsCacheKey(result), result);
    return result;
  }

  @Override
  public IssueCreationSettingsDto addDefaultSettings(String chatId) {
    checkAndFillCache();

    IssueCreationSettingsDto settings =
        IssueCreationSettingsDto.builder().chatId(chatId).enabled(false).tag("task").build();
    // IssueCreationSettingsDto settings = new IssueCreationSettingsDto();
    // settings.setEnabled(false);
    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.create(settings));
    issueSettingsCache.put(getSettingsCacheKey(result), result);
    return result;
  }

  @Override
  public IssueCreationSettingsDto updateSettings(int id, IssueCreationSettingsDto settings) {
    checkAndFillCache();
    IssueCreationSettingsDto oldSettings = getSettings(id);

    issueSettingsCache.remove(getSettingsCacheKey(oldSettings));

    IssueCreationSettingsDto result =
        new IssueCreationSettingsDto(issueCreationSettingsRepository.update(id, settings));
    issueSettingsCache.put(getSettingsCacheKey(result), result);
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
    checkAndFillCache();

    return issueSettingsCache.containsKey(getSettingsCacheKey(chatId, tag));
  }

  private void checkAndFillCache() {
    if (issueSettingsCache == null) {
      issueSettingsCache = new HashMap<>();
      issueCreationSettingsRepository
          .findAll()
          .forEach(
              settings ->
                  issueSettingsCache.put(
                      String.format("%s-%s", settings.getChatId(), settings.getTag()),
                      new IssueCreationSettingsDto(settings)));
    }
  }

  private String getSettingsCacheKey(IssueCreationSettingsDto settings) {
    return getSettingsCacheKey(settings.getChatId(), settings.getTag());
  }

  private String getSettingsCacheKey(String chatId, String tag) {
    return String.format("%s-%s", chatId, tag);
  }
}
