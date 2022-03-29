/* (C)2020 */
package ru.mail.jira.plugins.myteam.actions;

import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.myteam.commons.PermissionHelperService;

public class ProjectChatAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  private final ProjectService projectService;
  private final PermissionHelperService permissionHelperService;

  public ProjectChatAdminAction(
      @ComponentImport ProjectService projectService,
      PermissionHelperService permissionHelperService) {
    this.projectService = projectService;
    this.permissionHelperService = permissionHelperService;
  }

  @Override
  public String execute() {
    if (!permissionHelperService.isProjectAdmin(getLoggedInUser(), getProjectKey())) {
      return SECURITY_BREACH;
    }
    return SUCCESS;
  }

  public String getProjectKey() {
    return getHttpRequest().getParameter("project");
  }

  public Long getProjectId() {
    return projectService.getProjectByKey(getProjectKey()).get().getId();
  }
}
