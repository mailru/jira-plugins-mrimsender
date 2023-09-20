/* (C)2023 */
package ru.mail.jira.plugins.myteam.upgrades;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;

public class Version3UpgradeTask implements ActiveObjectsUpgradeTask {
  @Override
  public ModelVersion getModelVersion() {
    return ModelVersion.valueOf("3");
  }

  @Override
  public void upgrade(ModelVersion currentVersion, final ActiveObjects ao) {}
}
