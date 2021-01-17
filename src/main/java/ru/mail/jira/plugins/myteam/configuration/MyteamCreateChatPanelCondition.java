/* (C)2020 */
package ru.mail.jira.plugins.myteam.configuration;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.Condition;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.model.PluginData;

public class MyteamCreateChatPanelCondition implements Condition {
  private final PermissionManager permissionManager;
  private final PluginData pluginData;

  public MyteamCreateChatPanelCondition(
      @ComponentImport PermissionManager permissionManager, PluginData pluginData) {
    this.permissionManager = permissionManager;
    this.pluginData = pluginData;
  }

  @Override
  public void init(Map<String, String> map) throws PluginParseException {}

  @Override
  public boolean shouldDisplay(Map<String, Object> map) {
    ApplicationUser currentUser = (ApplicationUser) map.get("user");
    Issue currentIssue = (Issue) map.get("issue");
    if (currentIssue == null) return false;
    boolean isBotActive =
        StringUtils.isNotEmpty(pluginData.getToken())
            && StringUtils.isNotEmpty(pluginData.getBotApiUrl());
    boolean isChatCreationAllowedProject =
        pluginData.getChatCreationProjectIds().contains(currentIssue.getProjectId());
    return currentUser != null
        && currentUser.isActive()
        && isBotActive
        && isChatCreationAllowedProject
        && permissionManager.hasPermission(
            ProjectPermissions.EDIT_ISSUES, currentIssue, currentUser);
  }
}
