/* (C)2022 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.myteam.commons.PermissionHelperService;

public class ChatAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final PermissionHelperService permissionHelperService;

  public ChatAdminAction(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      PermissionHelperService permissionHelperService) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.permissionHelperService = permissionHelperService;
  }

  @Override
  public String execute() {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    String chatId = this.getHttpRequest().getParameter("chatId");

    if (user == null || !permissionHelperService.isChatAdminOrJiraAdmin(chatId, user.getEmailAddress())) {
      return SECURITY_BREACH;
    }

    return SUCCESS;
  }
}
