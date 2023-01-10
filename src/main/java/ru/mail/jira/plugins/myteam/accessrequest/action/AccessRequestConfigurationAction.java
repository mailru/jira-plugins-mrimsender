/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.action;

import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class AccessRequestConfigurationAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";
  private final PermissionHelper permissionHelper;

  public AccessRequestConfigurationAction(PermissionHelper permissionHelper) {
    this.permissionHelper = permissionHelper;
  }

  @Override
  public String execute() {
    if (!permissionHelper.isProjectAdmin(getLoggedInUser(), getProjectKey())) {
      return SECURITY_BREACH;
    }
    return SUCCESS;
  }

  public String getProjectKey() {
    return getHttpRequest().getParameter("project");
  }
}
