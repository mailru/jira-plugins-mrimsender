/* (C)2024 */
package ru.mail.jira.plugins.myteam.upgrades;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

@ExtendWith(MockitoExtension.class)
class Version7UpgradeTaskTest {

  @Mock
  @SuppressWarnings("NullAway")
  private ProjectManager projectManager;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private Version7UpgradeTask upgradeTask;

  @SuppressWarnings("NullAway")
  private ActiveObjects activeObjects;

  private ModelVersion modelVersion6;

  @BeforeEach
  void setUp() {
    activeObjects = mock(ActiveObjects.class);
    modelVersion6 = ModelVersion.valueOf("6");
  }

  @Test
  void upgradeWhenTableIsEmpty() {
    // GIVEN
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[0]);

    // WHEN
    upgradeTask.upgrade(modelVersion6, activeObjects);

    // THEN
    verify(activeObjects).migrate(same(IssueCreationSettings.class));
    verify(activeObjects).find(same(IssueCreationSettings.class), any(Query.class));
    verify(activeObjects, never()).delete(any(IssueCreationSettings[].class));
  }

  @Test
  void migrateWhenSettingHasNullProjectKey() {
    // GIVEN
    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getProjectKey()).thenReturn(null);
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    ArgumentCaptor<TransactionCallback<Void>> transactionCallbackArgumentCaptor =
        ArgumentCaptor.forClass(TransactionCallback.class);

    when(activeObjects.executeInTransaction(transactionCallbackArgumentCaptor.capture()))
        .thenReturn(null);

    // WHEN
    upgradeTask.upgrade(modelVersion6, activeObjects);
    transactionCallbackArgumentCaptor.getValue().doInTransaction();

    // THEN
    verify(activeObjects).migrate(same(IssueCreationSettings.class));
    verify(activeObjects, times(2)).find(same(IssueCreationSettings.class), any(Query.class));
    verify(projectManager, never()).getProjectObjByKey(anyString());
    verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
    verify(activeObjects).delete(same(issueCreationSettings));
  }

  @Test
  void migrateWhenProjectNotFoundByKeyFromSetting() {
    // GIVEN
    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getProjectKey()).thenReturn("PROJ");
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    when(projectManager.getProjectObjByKey(eq("PROJ"))).thenReturn(null);

    ArgumentCaptor<TransactionCallback<Void>> transactionCallbackArgumentCaptor =
        ArgumentCaptor.forClass(TransactionCallback.class);

    when(activeObjects.executeInTransaction(transactionCallbackArgumentCaptor.capture()))
        .thenReturn(null);

    // WHEN
    upgradeTask.upgrade(modelVersion6, activeObjects);
    transactionCallbackArgumentCaptor.getValue().doInTransaction();

    // THEN
    verify(activeObjects).migrate(same(IssueCreationSettings.class));
    verify(activeObjects, times(2)).find(same(IssueCreationSettings.class), any(Query.class));
    verify(projectManager).getProjectObjByKey(eq("PROJ"));
    verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
    verify(activeObjects).delete(same(issueCreationSettings));
  }

  @Test
  void migrateWhenSettingMigrated() {
    // GIVEN
    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getProjectKey()).thenReturn("PROJ");
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    Project project = mock(Project.class);
    when(project.getId()).thenReturn(10000L);
    when(projectManager.getProjectObjByKey(eq("PROJ"))).thenReturn(project);

    ArgumentCaptor<TransactionCallback<Void>> transactionCallbackArgumentCaptor =
        ArgumentCaptor.forClass(TransactionCallback.class);

    when(activeObjects.executeInTransaction(transactionCallbackArgumentCaptor.capture()))
        .thenReturn(null);

    // WHEN
    upgradeTask.upgrade(modelVersion6, activeObjects);
    transactionCallbackArgumentCaptor.getValue().doInTransaction();

    // THEN
    verify(activeObjects).migrate(same(IssueCreationSettings.class));
    verify(activeObjects, times(2)).find(same(IssueCreationSettings.class), any(Query.class));
    verify(projectManager).getProjectObjByKey(eq("PROJ"));
    verify(issueCreationSettings).setProjectId(eq(10000L));
    verify(issueCreationSettings).save();
    verify(activeObjects, times(1)).executeInTransaction(any(TransactionCallback.class));
    verify(activeObjects, never()).delete(any(IssueCreationSettings[].class));
  }
}
