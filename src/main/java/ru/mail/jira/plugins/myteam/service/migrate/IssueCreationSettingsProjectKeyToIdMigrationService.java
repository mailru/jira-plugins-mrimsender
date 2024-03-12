package ru.mail.jira.plugins.myteam.service.migrate;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Validated
@Slf4j
public class IssueCreationSettingsProjectKeyToIdMigrationService {
    private static final int BATCH_SIZE = 50;

    private final ActiveObjects activeObjects;
    private final ProjectManager projectManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final GlobalPermissionManager globalPermissionManager;

    @Autowired
    public IssueCreationSettingsProjectKeyToIdMigrationService(@ComponentImport final ActiveObjects activeObjects,
                                                               @ComponentImport final ProjectManager projectManager,
                                                               @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
                                                               @ComponentImport final GlobalPermissionManager globalPermissionManager) {
        this.activeObjects = activeObjects;
        this.projectManager = projectManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.globalPermissionManager = globalPermissionManager;
    }


    public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateAll() {
        validateLoggedInUser();

        int page = 0;
        IssueCreationSettings[] issueCreationSettings;

        final List<IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto> settingsNotMigrated = new ArrayList<>();
        final List<IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto> settingsMigrated = new ArrayList<>();

        while ((issueCreationSettings = findSettingsByOffset(page)) != null) {
            page++;
            for (final IssueCreationSettings settings : issueCreationSettings) {
                final String projectKey = settings.getProjectKey();
                final Project project = projectManager.getProjectObjByKey(projectKey);
                if (project == null) {
                    log.warn("Project by key {} not found in JIRA", projectKey);
                    settingsNotMigrated.add(IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), projectKey, "Project not found by key in JIRA"));
                    continue;
                }

                try {
                    migrate(settings, project.getKey(), project.getId());
                    settingsMigrated.add(IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), projectKey));
                } catch (Exception e) {
                    settingsNotMigrated.add(IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), projectKey, StringUtils.defaultString(e.getMessage())));
                }
            }
        }
        return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settingsNotMigrated, settingsMigrated);
    }

    public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateByProjectKey(@NotEmpty final String projectKey) {
        validateLoggedInUser();

        final IssueCreationSettings[] issueCreationSettings = activeObjects.find(IssueCreationSettings.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (issueCreationSettings.length == 0) {
            throw new NotFoundException(String.format("Settings by project key %s not found", projectKey));
        }

        final Project projectObjByKey = projectManager.getProjectObjByKey(projectKey);
        if (projectObjByKey == null) {
            return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(Arrays.stream(issueCreationSettings).map(settings -> IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), settings.getProjectKey(), "Project not found by key in JIRA")).collect(Collectors.toList()), Collections.emptyList());
        }

        final List<IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto> notMigratedSettings = new ArrayList<>();
        final List<IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto> migratedSettings = new ArrayList<>();

        for (final IssueCreationSettings settings : issueCreationSettings) {
            final Project project = projectManager.getProjectObjByKey(projectKey);
            try {
                migrate(settings, project.getKey(), project.getId());
                migratedSettings.add(IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), projectKey));
            } catch (Exception e) {
                notMigratedSettings.add(IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(settings.getID(), projectKey, StringUtils.defaultString(e.getMessage())));

            }
        }

        return IssueCreationSettingsProjectKeyToIdMigrationResultDto.of(notMigratedSettings, migratedSettings);
    }

    private void validateLoggedInUser() {
        if (!jiraAuthenticationContext.isLoggedInUser()) {
            throw new SecurityException();
        }

        final ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
        if (!globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, loggedInUser)) {
            throw new SecurityException(String.format("User %s has not permission to execute migration", loggedInUser.getEmailAddress()));
        }
    }

    private IssueCreationSettings[] findSettingsByOffset(final int page) {
        return activeObjects.find(IssueCreationSettings.class, Query.select().offset(page * BATCH_SIZE).limit(BATCH_SIZE).order("ID desc"));
    }

    private void migrate(final IssueCreationSettings settings, final String projectKey, final long projectId) {
        activeObjects.executeInTransaction(() -> {
            if (settings.getProjectKey() != null && !projectKey.equals(settings.getProjectKey())) {
                settings.setProjectKey(projectKey);
            }
            settings.setProjectId(projectId);
            settings.save();
            return null;
        });
    }

}
