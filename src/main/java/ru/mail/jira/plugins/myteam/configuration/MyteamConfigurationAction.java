/* (C)2020 */
package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.protocol.BotsOrchestrationService;

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
  private String token;
  private String botApiUrl;
  private String botName;
  private String botLink;
  private boolean enabledByDefault;
  private String notifiedUsers;

  private List<String> notifiedUserKeys;
  private Set<Long> excludingProjectIds;
  private Set<Long> chatCreationAllowedProjectIds;

  @Override
  public String doDefault() {
    token = pluginData.getToken();
    enabledByDefault = pluginData.isEnabledByDefault();
    botApiUrl = pluginData.getBotApiUrl();
    botName = pluginData.getBotName();
    botLink = pluginData.getBotLink();
    excludingProjectIds = pluginData.getExcludingProjectIds();
    chatCreationAllowedProjectIds = pluginData.getChatCreationProjectIds();
    notifiedUsers = CommonUtils.convertUserKeysToJoinedString(pluginData.getNotifiedUserKeys());
    return INPUT;
  }

  @RequiresXsrfCheck
  @Override
  protected String doExecute() {
    pluginData.setToken(token);
    pluginData.setBotApiUrl(botApiUrl);
    pluginData.setBotName(botName);
    pluginData.setBotLink(botLink);
    pluginData.setEnabledByDefault(enabledByDefault);
    pluginData.setExcludingProjectIds(excludingProjectIds);
    pluginData.setChatCreationProjectIds(chatCreationAllowedProjectIds);
    pluginData.setNotifiedUserKeys(notifiedUserKeys);

    saved = true;
    notifiedUsers = CommonUtils.convertUserKeysToJoinedString(notifiedUserKeys);

    botsOrchestrationService.restartAll();
    return INPUT;
  }

  @Override
  protected void doValidation() {
    if (StringUtils.isEmpty(token))
      addError("token", getText("ru.mail.jira.plugins.myteam.configuration.specifyToken"));
    if (StringUtils.isEmpty(botApiUrl))
      addError("botApiUrl", getText("ru.mail.jira.plugins.myteam.configuration.specifyBotApiUrl"));
    try {
      notifiedUserKeys = CommonUtils.convertJoinedStringToUserKeys(notifiedUsers);
    } catch (IllegalArgumentException e) {
      addError("notifiedUsers", e.getMessage());
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  public boolean isSaved() {
    return saved;
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
  public String getChatCreationAllowedProjectIds() {
    return CommonUtils.join(
        this.chatCreationAllowedProjectIds.stream()
            .map(String::valueOf)
            .collect(Collectors.toList()));
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setChatCreationAllowedProjectIds(String chatCreationAllowedProjectIds) {
    this.chatCreationAllowedProjectIds =
        StringUtils.isBlank(chatCreationAllowedProjectIds)
            ? Collections.emptySet()
            : CommonUtils.split(chatCreationAllowedProjectIds).stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
  }

  @SuppressWarnings("UnusedDeclaration")
  public List<Project> getChatCreationAllowedProjects() {
    return chatCreationAllowedProjectIds.stream()
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
