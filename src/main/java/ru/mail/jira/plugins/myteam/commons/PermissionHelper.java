/* (C)2020 */
package ru.mail.jira.plugins.myteam.commons;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.response.AdminsResponse;

@Slf4j
@Component
public final class PermissionHelper {

  private final GlobalPermissionManager globalPermissionManager;
  private final PermissionManager permissionManager;
  private final UserData userData;
  private final MyteamApiClient myteamClient;

  public PermissionHelper(
      @ComponentImport GlobalPermissionManager globalPermissionManager,
      @ComponentImport PermissionManager permissionManager,
      UserData userData,
      MyteamApiClient myteamClient) {
    this.globalPermissionManager = globalPermissionManager;
    this.permissionManager = permissionManager;
    this.userData = userData;
    this.myteamClient = myteamClient;
  }

  public boolean isChatAdminOrJiraAdmin(String chatId, String userId) {
    if (userId == null) {
      return false;
    }
    if (isJiraAdmin(userData.getUserByMrimLogin(userId))) {
      return true;
    }
    try {
      AdminsResponse response = myteamClient.getAdmins(chatId).getBody();
      return response.getAdmins() != null
          && response.getAdmins().stream().anyMatch(admin -> userId.equals(admin.getUserId()));
    } catch (MyteamServerErrorException e) {
      log.error("Unable to get chat admins", e);
      return false;
    }
  }


  public boolean isJiraAdmin(ApplicationUser user) {
    return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
  }

  public boolean isProjectAdmin(ApplicationUser user, Project project) {
    return isJiraAdmin(user)
        || permissionManager.hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user);
  }
}
