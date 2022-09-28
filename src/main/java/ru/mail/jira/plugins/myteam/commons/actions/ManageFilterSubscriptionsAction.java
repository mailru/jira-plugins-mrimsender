/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons.actions;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class ManageFilterSubscriptionsAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  @Override
  public String doExecute() {
    if (getLoggedInUser() == null) return SECURITY_BREACH;
    return SUCCESS;
  }
}
