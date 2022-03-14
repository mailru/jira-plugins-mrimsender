/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE;
import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_SUMMARY_TEMPLATE;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettings;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.GroupChatInfo;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.repository.IssueCreationSettingsRepository;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Service
@Slf4j
public class IssueCreationSettingsServiceImpl implements IssueCreationSettingsService {

  private static final String CACHE_NAME =
      IssueCreationSettingsServiceImpl.class.getName() + ".issueSettingsCache";

  private final IssueCreationSettingsRepository issueCreationSettingsRepository;
  private final IssueTypeManager issueTypeManager;
  private final ProjectService projectService;
  private final MyteamApiClient myteamApiClient;
  private final MessageFormatter messageFormatter;

  private final Cache<String, Optional<IssueCreationSettingsDto>> issueSettingsCache;

  public IssueCreationSettingsServiceImpl(
      IssueCreationSettingsRepository issueCreationSettingsRepository,
      MessageFormatter messageFormatter,
      MyteamApiClient myteamApiClient,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport ProjectService projectService,
      @ComponentImport CacheManager cacheManager) {
    this.issueCreationSettingsRepository = issueCreationSettingsRepository;
    this.issueTypeManager = issueTypeManager;
    this.projectService = projectService;
    this.messageFormatter = messageFormatter;
    this.myteamApiClient = myteamApiClient;

    issueSettingsCache =
        cacheManager.getCache(
            CACHE_NAME,
            this::getSettingsByChatId,
            new CacheSettingsBuilder().remote().replicateViaInvalidation().build());
  }

  @Override
  public List<IssueCreationSettingsDto> getAllSettings() {
    return mapAdditionalSettingsInfos(issueCreationSettingsRepository.findAll());
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

  @Override
  public List<IssueCreationSettingsDto> getSettingsByProjectId(long projectId) {
    Project project = projectService.getProjectById(projectId).getProject();
    if (project == null) {
      return new ArrayList<>();
    }
    return mapAdditionalSettingsInfos(
        issueCreationSettingsRepository.getSettingsByProjectId(project.getKey()));
  }

  @NotNull
  @Override
  public Optional<IssueCreationSettingsDto> getSettingsByChatId(String chatId) {
    return issueCreationSettingsRepository
        .getSettingsByChatId(chatId)
        .map(this::mapAdditionalSettingsInfo);
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

  @Override
  public IssueCreationSettingsDto getSettingsById(int id) {
    @NotNull IssueCreationSettings settings = issueCreationSettingsRepository.get(id);
    return new IssueCreationSettingsDto(
        settings, messageFormatter.getMyteamLink(settings.getChatId()));
  }

  private List<IssueCreationSettingsDto> mapAdditionalSettingsInfos(
      List<IssueCreationSettings> settings) {
    return settings.stream().map(this::mapAdditionalSettingsInfo).collect(Collectors.toList());
  }

  private IssueCreationSettingsDto mapAdditionalSettingsInfo(IssueCreationSettings settings) {

    IssueCreationSettingsDto settingsDto =
        new IssueCreationSettingsDto(
            settings, messageFormatter.getMyteamLink(settings.getChatId()));

    try {
      ChatInfoResponse chatInfo = myteamApiClient.getChatInfo(settings.getChatId()).getBody();
      if (chatInfo instanceof GroupChatInfo) {
        settingsDto.setChatTitle(((GroupChatInfo) chatInfo).getTitle());
      }
    } catch (MyteamServerErrorException e) {
      log.error(e.getLocalizedMessage(), e);
    }

    if (StringUtils.isNotEmpty(settings.getIssueTypeId())) {
      IssueType issueType = issueTypeManager.getIssueType(settings.getIssueTypeId());
      if (issueType != null) {
        settingsDto.setIssueTypeName(issueType.getNameTranslation());
      }
    }
    return settingsDto;
  }
}
