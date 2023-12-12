/* (C)2023 */
package ru.mail.jira.plugins.myteam.upgrades;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestConfiguration;

@Slf4j
public class Version5UpgradeTask implements ActiveObjectsUpgradeTask {

  @Override
  public ModelVersion getModelVersion() {
    return ModelVersion.valueOf("5");
  }

  @Override
  public void upgrade(ModelVersion modelVersion, ActiveObjects ao) {
    log.info("Current version " + modelVersion.toString());
    if (modelVersion.isOlderThan(getModelVersion())) {
      ao.migrate(AccessRequestConfiguration.class);
      ao.executeInTransaction(
          (TransactionCallback<Void>)
              () -> {
                for (AccessRequestConfiguration conf : ao.find(AccessRequestConfiguration.class)) {
                  if (conf.getAccessPermissionFields() == null
                      || conf.getAccessPermissionFields().isEmpty()) {
                    conf.setAccessPermissionFields("watchers");
                    conf.save();
                  }
                }
                return null;
              });
    }
  }
}
