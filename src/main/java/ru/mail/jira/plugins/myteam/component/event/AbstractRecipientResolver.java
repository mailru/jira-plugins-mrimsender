/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.notification.*;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.ofbiz.core.entity.GenericEntityException;
import ru.mail.jira.plugins.commons.SentryClient;

@Slf4j
public abstract class AbstractRecipientResolver<T, R> implements RecipientResolver<T, R> {
  private final NotificationFilterManager notificationFilterManager;
  private final NotificationSchemeManager notificationSchemeManager;
  private final PermissionManager permissionManager;
  private final ProjectRoleManager projectRoleManager;
  private final GroupManager groupManager;

  protected AbstractRecipientResolver(
      final NotificationFilterManager notificationFilterManager,
      final NotificationSchemeManager notificationSchemeManager,
      final PermissionManager permissionManager,
      final ProjectRoleManager projectRoleManager,
      final GroupManager groupManager) {
    this.notificationFilterManager = notificationFilterManager;
    this.notificationSchemeManager = notificationSchemeManager;
    this.permissionManager = permissionManager;
    this.projectRoleManager = projectRoleManager;
    this.groupManager = groupManager;
  }

  protected Set<NotificationRecipient> resolveRecipientByNotificationScheme(
      final IssueEvent issueEvent) throws GenericEntityException {
    final Set<NotificationRecipient> notificationRecipients = Sets.newHashSet();
    try {
      notificationRecipients.addAll(notificationSchemeManager.getRecipients(issueEvent));
    } catch (Exception e) {
      log.error("notificationSchemeManager.getRecipients({})", issueEvent, e);
    }

    NotificationFilterContext context =
        notificationFilterManager.makeContextFrom(JiraNotificationReason.ISSUE_EVENT, issueEvent);
    for (final SchemeEntity schemeEntity :
        notificationSchemeManager.getNotificationSchemeEntities(
            issueEvent.getProject(), issueEvent.getEventTypeId())) {
      context =
          notificationFilterManager.makeContextFrom(
              context,
              com.atlassian.jira.notification.type.NotificationType.from(schemeEntity.getType()));

      Set<NotificationRecipient> recipientsFromScheme = new HashSet<>();

      try {
        recipientsFromScheme = notificationSchemeManager.getRecipients(issueEvent, schemeEntity);
      } catch (NullPointerException e) {
        log.error(e.getLocalizedMessage(), e);
        SentryClient.capture(
            e,
            Map.of(
                "error",
                "Error while retrieving issue notification recipients. (Possibly related to components)",
                "issueKey",
                issueEvent.getIssue().getKey()));
      }

      recipientsFromScheme =
          Sets.newHashSet(
              notificationFilterManager.recomputeRecipients(recipientsFromScheme, context));
      notificationRecipients.addAll(recipientsFromScheme);
    }

    return notificationRecipients;
  }

  protected boolean canSendEventToUser(final ApplicationUser user, final IssueEvent issueEvent) {
    ProjectRole projectRole = null;
    String groupName = null;
    final Issue issue = issueEvent.getIssue();
    if (issueEvent.getWorklog() != null) {
      projectRole = issueEvent.getWorklog().getRoleLevel();
      groupName = issueEvent.getWorklog().getGroupLevel();
    } else if (issueEvent.getComment() != null) {
      projectRole = issueEvent.getComment().getRoleLevel();
      groupName = issueEvent.getComment().getGroupLevel();
    }

    if (!canSendEventToUser(user, issue)) {
      return false;
    }
    if (groupName != null && !groupManager.isUserInGroup(user, groupName)) {
      return false;
    }
    return projectRole == null
        || projectRoleManager.isUserInProjectRole(user, projectRole, issue.getProjectObject());
  }

  protected boolean canSendEventToUser(final ApplicationUser user, final Issue issue) {
    return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
  }
}
