/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons.actions;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.myteam.bot.BotsOrchestrationService;
import ru.mail.jira.plugins.myteam.service.PluginData;

public class MyteamConfigurationAction extends JiraWebActionSupport {
  private final PluginData pluginData;
  private final BotsOrchestrationService botsOrchestrationService;
  private final ProjectManager projectManager;

  public MyteamConfigurationAction(
      PluginData pluginData,
      BotsOrchestrationService botsOrchestrationService,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport GlobalPermissionManager globalPermissionManager) {
    this.botsOrchestrationService = botsOrchestrationService;
    this.pluginData = pluginData;
    this.projectManager = projectManager;
  }

  private boolean saved;
  private boolean setTokenViaFile;
  private String token;
  private String tokenFilePath;
  private String botApiUrl;
  private String botName;
  private String botLink;
  private String profileLink;
  private boolean enabledByDefault;
  private String notifiedUsers;

  private List<String> notifiedUserKeys;
  private Set<Long> excludingProjectIds;
  private Set<Long> chatCreationNotAllowedProjectIds;

  @Override
  public String doDefault() {
    setTokenViaFile = pluginData.isSetTokenViaFile();
    if (setTokenViaFile) tokenFilePath = pluginData.getTokenFilePath();
    else token = pluginData.getToken();
    enabledByDefault = pluginData.isEnabledByDefault();
    botApiUrl = pluginData.getBotApiUrl();
    botName = pluginData.getBotName();
    botLink = pluginData.getBotLink();
    profileLink = pluginData.getProfileLink();
    excludingProjectIds = pluginData.getExcludingProjectIds();
    chatCreationNotAllowedProjectIds = pluginData.getChatCreationBannedProjectIds();
    notifiedUsers = CommonUtils.convertUserKeysToJoinedString(pluginData.getNotifiedUserKeys());
    return INPUT;
  }

  @RequiresXsrfCheck
  @Override
  protected String doExecute() {
    // persist old token and api url values for bot smart restart decision making
    String prevToken = pluginData.getToken();
    String prevBotApiUrl = pluginData.getBotApiUrl();

    pluginData.setSetTokenViaFile(setTokenViaFile);
    if (setTokenViaFile) {
      pluginData.setTokenFilePath(tokenFilePath);
      try {
        String botToken = loadBotTokenFromFile(tokenFilePath);
        pluginData.setToken(botToken);
      } catch (IOException ioException) {
        log.error("Can't load bot token");
      }
    } else {
      pluginData.setToken(token);
    }
    pluginData.setBotApiUrl(botApiUrl);
    pluginData.setBotName(botName);
    pluginData.setBotLink(botLink);
    pluginData.setProfileLink(profileLink);
    pluginData.setEnabledByDefault(enabledByDefault);
    pluginData.setExcludingProjectIds(excludingProjectIds);
    pluginData.setChatCreationBannedProjectIds(chatCreationNotAllowedProjectIds);
    pluginData.setNotifiedUserKeys(notifiedUserKeys);

    saved = true;
    notifiedUsers = CommonUtils.convertUserKeysToJoinedString(notifiedUserKeys);

    boolean shouldRestartBot =
        prevToken == null
            || prevBotApiUrl == null
            || !prevToken.equals(token)
            || !prevBotApiUrl.equals(botApiUrl);
    if (shouldRestartBot) botsOrchestrationService.restartAll();
    return INPUT;
  }

  @Override
  protected void doValidation() {
    if (setTokenViaFile) {
      if (StringUtils.isEmpty(tokenFilePath))
        addError(
            "tokenFilePath",
            getText("ru.mail.jira.plugins.myteam.configuration.tokenFilePath.isEmptyError"));
      else {
        File file = new File(tokenFilePath);
        if (file.isDirectory())
          addError(
              "tokenFilePath",
              getText(
                  "ru.mail.jira.plugins.myteam.configuration.tokenFilePath.shouldNotBeDirectoryError"));
        else if (!file.exists()) {
          addError(
              "tokenFilePath",
              getText("ru.mail.jira.plugins.myteam.configuration.tokenFilePath.fileNotExistError"));
        } else if (!file.canRead())
          addError(
              "tokenFilePath",
              getText(
                  "ru.mail.jira.plugins.myteam.configuration.tokenFilePath.readPermissionsError"));
      }
    } else {
      if (StringUtils.isEmpty(token))
        addError("token", getText("ru.mail.jira.plugins.myteam.configuration.specifyToken"));
    }

    if (StringUtils.isEmpty(botApiUrl))
      addError("botApiUrl", getText("ru.mail.jira.plugins.myteam.configuration.specifyBotApiUrl"));
    try {
      notifiedUserKeys = CommonUtils.convertJoinedStringToUserKeys(notifiedUsers);
    } catch (IllegalArgumentException e) {
      addError("notifiedUsers", e.getMessage());
    }
  }

  @Nullable
  private String loadBotTokenFromFile(String tokenFilePath) throws IOException {
    Properties myteamBotPropeties = new Properties();
    InputStream inputStream = new FileInputStream(tokenFilePath);
    myteamBotPropeties.load(inputStream);
    String token = myteamBotPropeties.getOrDefault("token", "").toString();
    inputStream.close();
    return token;
  }

  @SuppressWarnings("UnusedDeclaration")
  public boolean isSaved() {
    return saved;
  }

  @SuppressWarnings("UnusedDeclaration")
  public boolean isSetTokenViaFile() {
    return setTokenViaFile;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setSetTokenViaFile(boolean setTokenViaFile) {
    this.setTokenViaFile = setTokenViaFile;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getToken() {
    return token;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setToken(String token) {
    this.token = token;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getTokenFilePath() {
    return tokenFilePath;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setTokenFilePath(String tokenFilePath) {
    this.tokenFilePath = tokenFilePath;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getBotApiUrl() {
    return botApiUrl;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setBotApiUrl(String botApiUrl) {
    this.botApiUrl = botApiUrl;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getBotName() {
    return botName;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setBotName(String botName) {
    this.botName = botName;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getBotLink() {
    return botLink;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setBotLink(String botLink) {
    this.botLink = botLink;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getProfileLink() {
    return profileLink;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setProfileLink(String profileLink) {
    this.profileLink = profileLink;
  }

  @SuppressWarnings("UnusedDeclaration")
  public boolean isEnabledByDefault() {
    return enabledByDefault;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setEnabledByDefault(boolean enabledByDefault) {
    this.enabledByDefault = enabledByDefault;
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getExcludingProjectIds() {
    return CommonUtils.join(
        this.excludingProjectIds.stream().map(String::valueOf).collect(Collectors.toList()));
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setExcludingProjectIds(String excludingProjectIds) {
    this.excludingProjectIds =
        StringUtils.isBlank(excludingProjectIds)
            ? Collections.emptySet()
            : CommonUtils.split(excludingProjectIds).stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
  }

  @SuppressWarnings("UnusedDeclaration")
  public List<Project> getExcludingProjects() {
    return excludingProjectIds.stream()
        .map(projectManager::getProjectObj)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getChatCreationNotAllowedProjectIds() {
    return CommonUtils.join(
        this.chatCreationNotAllowedProjectIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList()));
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setChatCreationNotAllowedProjectIds(String chatCreationNotAllowedProjectIds) {
    this.chatCreationNotAllowedProjectIds =
        StringUtils.isBlank(chatCreationNotAllowedProjectIds)
            ? Collections.emptySet()
            : CommonUtils.split(chatCreationNotAllowedProjectIds).stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
  }

  @SuppressWarnings("UnusedDeclaration")
  public List<Project> getChatCreationNotAllowedProjects() {
    return chatCreationNotAllowedProjectIds.stream()
        .map(projectManager::getProjectObj)
        .collect(Collectors.toList());
  }

  @SuppressWarnings("UnusedDeclaration")
  public Collection<Project> getProjects() {
    return projectManager.getProjects();
  }

  @SuppressWarnings("UnusedDeclaration")
  public String getNotifiedUsers() {
    return notifiedUsers;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setNotifiedUsers(String notifiedUsers) {
    this.notifiedUsers = notifiedUsers;
  }
}
