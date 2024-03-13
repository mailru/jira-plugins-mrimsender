/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.migrate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto;
import ru.mail.jira.plugins.myteam.db.model.IssueCreationSettings;

@ExtendWith(MockitoExtension.class)
class IssueCreationSettingsProjectKeyToIdMigrationServiceTest {

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private ActiveObjects activeObjects;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private ProjectService projectService;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private JiraAuthenticationContext jiraAuthenticationContext;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private GlobalPermissionManager globalPermissionManager;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private IssueCreationSettingsProjectKeyToIdMigrationService
      issueCreationSettingsProjectKeyToIdMigrationService;

  @Test
  void migrateAllWhenUserNotLoggedIn() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(false);

    // WHEN // THEN
    assertThrows(
        SecurityException.class, issueCreationSettingsProjectKeyToIdMigrationService::migrateAll);
  }

  @Test
  void migrateAllWhenUserNotAdmin() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(false);
    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.SYSTEM_ADMIN), same(loggedInUser)))
        .thenReturn(false);

    // WHEN // THEN
    assertThrows(
        SecurityException.class, issueCreationSettingsProjectKeyToIdMigrationService::migrateAll);
  }

  @Test
  void migrateAllWhenSettingsTableIsEmpty() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(true);

    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[0]);

    // WHEN
    IssueCreationSettingsProjectKeyToIdMigrationResultDto resultDto =
        issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();

    // THEN
    assertTrue(resultDto.getMigratedSettings().isEmpty());
    assertTrue(resultDto.getNotMigratedSettings().isEmpty());
    verify(projectService, never()).getProjectByKey(anyString());
    verify(activeObjects, never()).executeInTransaction(any(TransactionCallback.class));
  }

  @Test
  void migrateWhenSettingHasNullProjectKey() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(true);

    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getID()).thenReturn(1);
    when(issueCreationSettings.getProjectKey()).thenReturn(null);
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    // WHEN
    IssueCreationSettingsProjectKeyToIdMigrationResultDto resultDto =
        issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();

    // THEN
    assertTrue(resultDto.getMigratedSettings().isEmpty());
    assertFalse(resultDto.getNotMigratedSettings().isEmpty());
    IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto
        issueCreationSettingsMigratedInfoDto = resultDto.getNotMigratedSettings().get(0);
    assertNotNull(issueCreationSettingsMigratedInfoDto);
    assertEquals(1, issueCreationSettingsMigratedInfoDto.getId());
    assertNull(issueCreationSettingsMigratedInfoDto.getProjectKey());
    assertEquals("Settings has null project key", issueCreationSettingsMigratedInfoDto.getError());

    verify(projectService, never()).getProjectByKey(anyString());
    verify(activeObjects, never()).executeInTransaction(any(TransactionCallback.class));
  }

  @Test
  void migrateWhenProjectNotFoundByKeyFromSetting() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(true);

    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getID()).thenReturn(1);
    when(issueCreationSettings.getProjectKey()).thenReturn("PROJ");
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);
    when(projectService.getProjectByKey(eq("PROJ")))
        .thenReturn(new ProjectService.GetProjectResult(new SimpleErrorCollection(), null));

    // WHEN
    IssueCreationSettingsProjectKeyToIdMigrationResultDto resultDto =
        issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();

    // THEN
    assertTrue(resultDto.getMigratedSettings().isEmpty());
    assertFalse(resultDto.getNotMigratedSettings().isEmpty());
    IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto
        issueCreationSettingsMigratedInfoDto = resultDto.getNotMigratedSettings().get(0);
    assertNotNull(issueCreationSettingsMigratedInfoDto);
    assertEquals(1, issueCreationSettingsMigratedInfoDto.getId());
    assertEquals("PROJ", issueCreationSettingsMigratedInfoDto.getProjectKey());
    assertEquals(
        "Project not found by key in JIRA", issueCreationSettingsMigratedInfoDto.getError());

    verify(projectService).getProjectByKey(eq("PROJ"));
    verify(activeObjects, never()).executeInTransaction(any(TransactionCallback.class));
  }

  @Test
  void migrateWhenMigratingSettingsHappenedErrorOnSaving() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(true);

    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getID()).thenReturn(1);
    when(issueCreationSettings.getProjectKey()).thenReturn("PROJ");
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    Project project = mock(Project.class);
    when(project.getId()).thenReturn(10000L);
    when(projectService.getProjectByKey(eq("PROJ")))
        .thenReturn(new ProjectService.GetProjectResult(project));

    ArgumentCaptor<TransactionCallback<Void>> transactionCallbackArgumentCaptor =
        ArgumentCaptor.forClass(TransactionCallback.class);

    when(activeObjects.executeInTransaction(transactionCallbackArgumentCaptor.capture()))
        .thenThrow(new DataAccessException("Some error in database"));

    // WHEN
    IssueCreationSettingsProjectKeyToIdMigrationResultDto resultDto =
        issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();
    transactionCallbackArgumentCaptor.getValue().doInTransaction();

    // THEN
    assertTrue(resultDto.getMigratedSettings().isEmpty());

    assertFalse(resultDto.getNotMigratedSettings().isEmpty());
    IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto
        issueCreationSettingNotMigratedInfoDto = resultDto.getNotMigratedSettings().get(0);
    assertNotNull(issueCreationSettingNotMigratedInfoDto);
    assertEquals(1, issueCreationSettingNotMigratedInfoDto.getId());
    assertEquals("PROJ", issueCreationSettingNotMigratedInfoDto.getProjectKey());
    assertEquals("Some error in database", issueCreationSettingNotMigratedInfoDto.getError());

    verify(projectService).getProjectByKey(eq("PROJ"));
    verify(project).getId();
    verify(issueCreationSettings).setProjectId(eq(10000L));
    verify(issueCreationSettings).save();
    verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
  }

  @Test
  void migrateWhenMigratingSuccess() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    when(globalPermissionManager.hasPermission(
            same(GlobalPermissionKey.ADMINISTER), same(loggedInUser)))
        .thenReturn(true);

    IssueCreationSettings issueCreationSettings = mock(IssueCreationSettings.class);
    when(issueCreationSettings.getID()).thenReturn(1);
    when(issueCreationSettings.getProjectKey()).thenReturn("PROJ");
    when(activeObjects.find(same(IssueCreationSettings.class), any(Query.class)))
        .thenReturn(new IssueCreationSettings[] {issueCreationSettings})
        .thenReturn(new IssueCreationSettings[0]);

    Project project = mock(Project.class);
    when(project.getId()).thenReturn(10000L);
    when(projectService.getProjectByKey(eq("PROJ")))
        .thenReturn(new ProjectService.GetProjectResult(project));

    ArgumentCaptor<TransactionCallback<Void>> transactionCallbackArgumentCaptor =
        ArgumentCaptor.forClass(TransactionCallback.class);

    when(activeObjects.executeInTransaction(transactionCallbackArgumentCaptor.capture()))
        .thenReturn(null);

    // WHEN
    IssueCreationSettingsProjectKeyToIdMigrationResultDto resultDto =
        issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();
    transactionCallbackArgumentCaptor.getValue().doInTransaction();

    // THEN
    assertTrue(resultDto.getNotMigratedSettings().isEmpty());

    assertFalse(resultDto.getMigratedSettings().isEmpty());
    IssueCreationSettingsProjectKeyToIdMigrationResultDto.IssueCreationSettingsMigratedInfoDto
        issueCreationSettingsMigratedInfoDto = resultDto.getMigratedSettings().get(0);
    assertNotNull(issueCreationSettingsMigratedInfoDto);
    assertEquals(1, issueCreationSettingsMigratedInfoDto.getId());
    assertEquals("PROJ", issueCreationSettingsMigratedInfoDto.getProjectKey());
    assertNull(issueCreationSettingsMigratedInfoDto.getError());

    verify(projectService).getProjectByKey(eq("PROJ"));
    verify(project).getId();
    verify(issueCreationSettings).setProjectId(eq(10000L));
    verify(issueCreationSettings).save();
    verify(activeObjects).executeInTransaction(any(TransactionCallback.class));
  }
}
