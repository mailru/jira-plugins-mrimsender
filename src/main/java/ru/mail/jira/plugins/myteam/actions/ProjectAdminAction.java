/* (C)2020 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.myteam.commons.PermissionHelper;

public class ProjectAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  private final ProjectService projectService;
  private final PermissionHelper permissionHelper;

  public ProjectAdminAction(
      @ComponentImport ProjectService projectService, PermissionHelper permissionHelper) {
    this.projectService = projectService;
    this.permissionHelper = permissionHelper;
  }

  @Override
  public String execute() {
    Project project = getProject();

    if (project == null || !permissionHelper.isProjectAdmin(getLoggedInUser(), project)) {
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
