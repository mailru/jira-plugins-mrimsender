/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons.actions;

import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class ProjectChatAdminAction extends JiraWebActionSupport {
  private static final String SECURITY_BREACH = "securitybreach";

  private final ProjectService projectService;
  private final PermissionHelper permissionHelper;

  public ProjectChatAdminAction(
      @ComponentImport ProjectService projectService, PermissionHelper permissionHelper) {
    this.projectService = projectService;
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

  public Long getProjectId() {
    return projectService.getProjectByKey(getProjectKey()).get().getId();
  }
}
