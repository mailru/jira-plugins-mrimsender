/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons.actions;

import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.JiraWebActionSupport;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class ManageFilterSubscriptionsAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  @Override
  public String doExecute() {
    if (getLoggedInUser() == null) return SECURITY_BREACH;
    return SUCCESS;
  }
}
