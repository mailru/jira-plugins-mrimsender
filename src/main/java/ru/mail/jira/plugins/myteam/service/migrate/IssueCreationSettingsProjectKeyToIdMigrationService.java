/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.migrate;

import static ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

@Component
@Slf4j
public class IssueCreationSettingsProjectKeyToIdMigrationService {
  private static final int BATCH_SIZE = 50;

  private final ActiveObjects activeObjects;
  private final ProjectService projectService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final GlobalPermissionManager globalPermissionManager;

  @Autowired
  public IssueCreationSettingsProjectKeyToIdMigrationService(
      @ComponentImport final ActiveObjects activeObjects,
      @ComponentImport final ProjectService projectService,
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport final GlobalPermissionManager globalPermissionManager) {
    this.activeObjects = activeObjects;
    this.projectService = projectService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.globalPermissionManager = globalPermissionManager;
  }

  public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateAll() {
    validateLoggedInUser();

    int page = 0;
    IssueCreationSettings[] issueCreationSettings;

    final List<IssueCreationSettingsMigratedInfoDto> settingsNotMigrated = new ArrayList<>();
    final List<IssueCreationSettingsMigratedInfoDto> settingsMigrated = new ArrayList<>();

    while ((issueCreationSettings = findSettingsByOffset(page)).length != 0) {
      page++;
      for (final IssueCreationSettings settings : issueCreationSettings) {
        final String projectKey = settings.getProjectKey();
        if (projectKey == null) {
          settingsNotMigrated.add(
              IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(
                  settings.getID(), null, "Settings has null project key"));
          continue;
        }

        final Project project = projectService.getProjectByKey(projectKey).getProject();
        if (project == null) {
          log.warn("Project by key {} not found in JIRA", projectKey);
          settingsNotMigrated.add(
              IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(
                  settings.getID(), projectKey, "Project not found by key in JIRA"));
          continue;
        }

        try {
          activeObjects.executeInTransaction(
              () -> {
                settings.setProjectId(project.getId());
                settings.save();
                return null;
              });
          settingsMigrated.add(
              IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(
                  settings.getID(), projectKey));
        } catch (Exception e) {
          log.error(
              "error happened during migrating issue creation settings entity {}",
              settings.getID(),
              e);
          settingsNotMigrated.add(
              IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(
                  settings.getID(), projectKey, StringUtils.defaultString(e.getMessage())));
        }
      }
    }
    return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(
        settingsNotMigrated, settingsMigrated);
  }

  private void validateLoggedInUser() {
    if (!jiraAuthenticationContext.isLoggedInUser()) {
      log.error("unknown user");
      throw new SecurityException();
    }

    final ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (!globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser)
        && !globalPermissionManager.hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, loggedInUser)) {
      log.error(
          "User {} has not admin permission to execute migration", loggedInUser.getEmailAddress());
      throw new SecurityException(
          String.format(
              "User %s has not admin permission to execute migration",
              loggedInUser.getEmailAddress()));
    }
  }

  private IssueCreationSettings[] findSettingsByOffset(final int page) {
    return activeObjects.find(
        IssueCreationSettings.class,
        Query.select().offset(page * BATCH_SIZE).limit(BATCH_SIZE).order("ID desc"));
  }
}
