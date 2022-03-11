/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.RestFieldException;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.response.AdminsResponse;

@Slf4j
@Component
public class PermissionHelper {

  private final GlobalPermissionManager globalPermissionManager;
  private final PermissionManager permissionManager;
  private final ProjectManager projectManager;
  private final UserData userData;
  private final MyteamApiClient myteamClient;

  public PermissionHelper(
      @ComponentImport GlobalPermissionManager globalPermissionManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport ProjectManager projectManager,
      UserData userData,
      MyteamApiClient myteamClient) {
    this.globalPermissionManager = globalPermissionManager;
    this.permissionManager = permissionManager;
    this.projectManager = projectManager;
    this.userData = userData;
    this.myteamClient = myteamClient;
  }

  public boolean isChatAdminOrJiraAdmin(String chatId, String userId) {
    if (userId == null) {
      return false;
    }
    return isChatAdminOrJiraAdmin(chatId, userData.getUserByMrimLogin(userId));
  }

  public boolean isChatAdminOrJiraAdmin(String chatId, ApplicationUser user) {
    if (user == null) {
      return false;
    }
    if (isJiraAdmin(user)) {
      return true;
    }
    try {
      AdminsResponse response = myteamClient.getAdmins(chatId).getBody();
      return response.getAdmins() != null
          && response.getAdmins().stream()
              .anyMatch(admin -> user.getEmailAddress().equals(admin.getUserId()));
    } catch (MyteamServerErrorException e) {
      log.error("Unable to get chat admins", e);
      return false;
    }
  }

  public void checkIfProjectAdmin(ApplicationUser user, Project project) {
    if (!isProjectAdmin(user, project)) {
      throw new SecurityException();
    }
  }

  public boolean isJiraAdmin(ApplicationUser user) {
    return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
  }

  public boolean isProjectAdmin(ApplicationUser user, long projectId) {
    try {
      return isProjectAdmin(user, getExistingProject(projectId));
    } catch (Exception e) {
      log.error("calling isProjectAdmin({},{})", user, projectId, e);
      return false;
    }
  }

  public boolean isProjectAdmin(ApplicationUser user, Project project) {
    return isJiraAdmin(user)
        || permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user);
  }

  private Project getExistingProject(long projectId) {
    Project project = projectManager.getProjectObj(projectId);
    if (project == null) {
      throw new RestFieldException("Project doesn't exist");
    }
    return project;
  }

  public ApplicationUser checkChatAdminPermissions(ApplicationUser user)
      throws PermissionException {
    return checkChatAdminPermissions(user, null);
  }

  public ApplicationUser checkChatAdminPermissions(ApplicationUser user, @Nullable String chatId)
      throws PermissionException {
    if (isChatAdminOrJiraAdmin(chatId, user)) {
      return user;
    }
    throw new PermissionException();
  }

  public ApplicationUser checkProjectPermissions(ApplicationUser user, Long projectId)
      throws PermissionException {

    if (isProjectAdmin(user, projectId)) {
      return user;
    }
    throw new PermissionException();
  }
}
