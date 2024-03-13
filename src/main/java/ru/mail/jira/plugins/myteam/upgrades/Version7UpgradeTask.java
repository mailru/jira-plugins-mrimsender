/* (C)2024 */
package ru.mail.jira.plugins.myteam.upgrades;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.Entity;
import net.java.ao.Query;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

@Slf4j
public class Version7UpgradeTask implements ActiveObjectsUpgradeTask {

  private final ProjectManager projectManager;

  public Version7UpgradeTask(@ComponentImport final ProjectManager projectManager) {
    this.projectManager = projectManager;
  }

  @Override
  public ModelVersion getModelVersion() {
    return ModelVersion.valueOf("7");
  }

  @Override
  public void upgrade(final ModelVersion currentVersion, final ActiveObjects activeObjects) {
    log.info("Current version " + currentVersion.toString());
    if (currentVersion.isOlderThan(getModelVersion())) {
      activeObjects.migrate(IssueCreationSettings.class);
      int page = 0;
      IssueCreationSettings[] issueCreationSettings;
      final List<IssueCreationSettings> settingsWithNotFoundProjectByKey = new ArrayList<>();
      while ((issueCreationSettings = findSettingsByOffset(page, activeObjects)).length != 0) {
        page++;
        for (final IssueCreationSettings settings : issueCreationSettings) {
          final String projectKey = settings.getProjectKey();
          if (projectKey == null) {
            log.warn("Settings with id {} has null project key", settings.getID());
            settingsWithNotFoundProjectByKey.add(settings);
            continue;
          }

          final Project project = projectManager.getProjectObjByKey(projectKey);
          if (project == null) {
            log.warn("Project by key {} not found in JIRA", projectKey);
            settingsWithNotFoundProjectByKey.add(settings);
            continue;
          }

          try {
            activeObjects.executeInTransaction(
                () -> {
                  settings.setProjectId(project.getId());
                  settings.save();
                  return null;
                });
          } catch (Exception e) {
            log.error(
                "error happened during migrating issue creation settings entity {}",
                settings.getID(),
                e);
          }
        }
      }

      if (settingsWithNotFoundProjectByKey.isEmpty()) {
        return;
      }

      try {
        activeObjects.executeInTransaction(
            () -> {
              activeObjects.delete(
                  settingsWithNotFoundProjectByKey.toArray(new IssueCreationSettings[0]));
              return null;
            });
      } catch (Exception e) {
        log.error(
            "error happened during deleting wrong records with ids {}",
            Arrays.stream(issueCreationSettings).map(Entity::getID).collect(Collectors.toList()),
            e);
      }
    }
  }

  private IssueCreationSettings[] findSettingsByOffset(
      final int page, ActiveObjects activeObjects) {
    return activeObjects.find(
        IssueCreationSettings.class, Query.select().offset(page * 50).limit(50).order("ID desc"));
  }
}
