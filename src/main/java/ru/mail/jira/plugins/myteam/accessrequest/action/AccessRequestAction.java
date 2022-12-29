/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.action;

import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.JiraWebActionSupport;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class AccessRequestAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  @Override
  public String execute() {
    if (getLoggedInUser() == null) return SECURITY_BREACH;
    return SUCCESS;
  }

  public String getIssueKey() {
    return getHttpRequest().getParameter("issueKey");
  }
}
