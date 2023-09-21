/* (C)2023 */
package ru.mail.jira.plugins.myteam.upgrades;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import lombok.extern.slf4j.Slf4j;
import net.java.ao.Query;
import ru.mail.jira.plugins.myteam.db.model.AdditionalIssueField;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

@Slf4j
public class Version3UpgradeTask implements ActiveObjectsUpgradeTask {
  @Override
  public ModelVersion getModelVersion() {
    return ModelVersion.valueOf("3");
  }

  @Override
  public void upgrade(ModelVersion currentVersion, final ActiveObjects ao) {
    log.info("Current version " + currentVersion.toString());
    if (currentVersion.isOlderThan(getModelVersion())) {
      ao.migrate(IssueCreationSettings.class);
      log.info("Run upgrade task to version 3");
      ao.executeInTransaction(
              (TransactionCallback<Void>)
                      () -> {
                        for (AdditionalIssueField field :
                                ao.find(
                                        AdditionalIssueField.class,
                                        Query.select().where("FIELD_ID = ?", "assignee"))) {

                          IssueCreationSettings settings = field.getIssueCreationSettings();

                          settings.setAssignee(field.getValue());
                          settings.save();

                          ao.delete(field);
                        }
                        return null;
                      });
    }
  }
}
