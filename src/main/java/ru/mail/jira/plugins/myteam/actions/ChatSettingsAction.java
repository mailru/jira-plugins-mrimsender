/* (C)2022 */
package ru.mail.jira.plugins.myteam.actions;

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

  public ChatSettingsAction(
      MyteamApiClient myteamClient,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    this.myteamClient = myteamClient;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
  }

  @Override
  public String execute() {
    String query = this.getHttpRequest().getQueryString();
    Matcher matcher = pattern.matcher(query);
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

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
}
