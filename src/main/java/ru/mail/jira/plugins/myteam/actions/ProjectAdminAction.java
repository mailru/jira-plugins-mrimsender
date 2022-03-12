/* (C)2020 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.commons.PermissionHelperService;

public class ProjectAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  private final PermissionHelperService permissionHelperService;

  public ProjectAdminAction(PermissionHelperService permissionHelperService) {
    this.permissionHelperService = permissionHelperService;
  }

  @Override
  public String execute() {
    String project = getHttpRequest().getParameter("project");
    if (!StringUtils.isNumeric(project)
        || !permissionHelperService.isProjectAdmin(getLoggedInUser(), Long.parseLong(project))) {
      return SECURITY_BREACH;
    }
    return SUCCESS;
  }
}
