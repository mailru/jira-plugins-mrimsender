/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.notification.NotificationFilterManager;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.ofbiz.core.entity.GenericEntityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.AbstractRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.RecipientResolver;

@Component
public class RemoteIssueLinkEventRecipientResolver
    extends AbstractRecipientResolver<RemoteIssueLinkData, Set<EventRecipient>>
    implements RecipientResolver<RemoteIssueLinkData, Set<EventRecipient>> {

  private final UserData userData;

  @Autowired
  public RemoteIssueLinkEventRecipientResolver(
      @ComponentImport final NotificationFilterManager notificationFilterManager,
      @ComponentImport final NotificationSchemeManager notificationSchemeManager,
      @ComponentImport final PermissionManager permissionManager,
      @ComponentImport final ProjectRoleManager projectRoleManager,
      @ComponentImport final GroupManager groupManager,
      final UserData userData) {
    super(
        notificationFilterManager,
        notificationSchemeManager,
        permissionManager,
        projectRoleManager,
        groupManager);
    this.userData = userData;
  }

  @Override
  public Set<EventRecipient> resolve(final RemoteIssueLinkData remoteIssueLinkData) {
    return resolve(
        new IssueEvent(
            remoteIssueLinkData.getIssue(),
            Map.of(),
            remoteIssueLinkData.getLinkCreator(),
            EventType.ISSUE_UPDATED_ID));
  }

  @NotNull
  private Set<EventRecipient> resolve(final IssueEvent syntheticEventOnCreatedRemoteLink) {
    final Issue issue = syntheticEventOnCreatedRemoteLink.getIssue();
    try {
      final Set<NotificationRecipient> notificationRecipients =
          super.resolveRecipientByNotificationScheme(syntheticEventOnCreatedRemoteLink);
      return notificationRecipients.stream()
          .map(NotificationRecipient::getUser)
          .filter(
              user ->
                  userData.isLinkNotificationEnable(user) && super.canSendEventToUser(user, issue))
          .map(EventRecipient::of)
          .collect(Collectors.toSet());
    } catch (GenericEntityException e) {
      SentryClient.capture(e, Map.of("issueKey", issue.getKey()));
      return Set.of();
    }
  }
}
