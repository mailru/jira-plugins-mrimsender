/* (C)2022 */
package ru.mail.jira.plugins.myteam.upgrades;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettings;

@Slf4j
public class Version2UpgradeTask implements ActiveObjectsUpgradeTask {
  @Override
  public ModelVersion getModelVersion() {
    return ModelVersion.valueOf("2");
  }

  @Override
  public void upgrade(ModelVersion currentVersion, final ActiveObjects ao) {
    log.info("Current version " + currentVersion.toString());
    if (currentVersion.isOlderThan(getModelVersion())) {
      ao.migrate(IssueCreationSettings.class);
      log.info("Run upgrade task to version 2");
      ao.executeInTransaction(
          (TransactionCallback<Void>)
              () -> {
                for (IssueCreationSettings settings : ao.find(IssueCreationSettings.class)) {
                  settings.setAddReporterInWatchers(Boolean.TRUE);
                  settings.save();
                }
                return null;
              });
    }
  }
}
