/* (C)2020 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import ru.mail.jira.plugins.myteam.commons.PermissionHelperService;

public class ProjectAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  private final ProjectService projectService;
  private final PermissionHelperService permissionHelperService;

  public ProjectAdminAction(
      ProjectService projectService, PermissionHelperService permissionHelperService) {
    this.projectService = projectService;
    this.permissionHelperService = permissionHelperService;
  }

  @Override
  public String execute() {
    Project project = getProject();

    if (project == null || !permissionHelperService.isProjectAdmin(getLoggedInUser(), project)) {
      return SECURITY_BREACH;
    }
    return SUCCESS;
  }

  private Project getProject() {
    String project = getHttpRequest().getParameter("project");
    return projectService.getProjectById(getLoggedInUser(), Long.parseLong(project)).getProject();
  }

  public String getProjectKey() {
    Project project = getProject();
    return project != null ? project.getKey() : null;
  }
}
