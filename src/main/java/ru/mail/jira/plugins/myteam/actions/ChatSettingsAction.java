/* (C)2022 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.response.AdminsResponse;

public class ChatSettingsAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";
  private static final Pattern pattern = Pattern.compile("chatId=([^&]+)");
  private final MyteamApiClient myteamClient;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final GlobalPermissionManager globalPermissionManager;

  public ChatSettingsAction(
      MyteamApiClient myteamClient,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport GlobalPermissionManager globalPermissionManager) {
    this.myteamClient = myteamClient;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.globalPermissionManager = globalPermissionManager;
  }

  @Override
  public String execute() {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

    if (isJiraAdmin(user)) {
      return SUCCESS;
    }
    String query = this.getHttpRequest().getQueryString();
    Matcher matcher = pattern.matcher(query);

    if (matcher.find()) {
      if (!checkPermissions(matcher.group(1), user.getEmailAddress())) return SECURITY_BREACH;
    }
    return SUCCESS;
  }

  private boolean checkPermissions(String chatId, String userId) throws SecurityException {
    try {
      AdminsResponse response = myteamClient.getAdmins(chatId).getBody();
      return response.getAdmins().stream().anyMatch(admin -> admin.getUserId().equals(userId));
    } catch (MyteamServerErrorException e) {
      log.error("Unable to get chat admins", e);
      return false;
    }
  }

  private boolean isJiraAdmin(ApplicationUser user) {
    return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
  }
}
