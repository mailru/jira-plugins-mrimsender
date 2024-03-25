/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.notification.NotificationFilterManager;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.ofbiz.core.entity.GenericEntityException;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.AbstractRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;

@Component
public class IssueLinkEventRecipientResolver
    extends AbstractRecipientResolver<IssueLinkEventData, IssueLinkEventRecipientsData> {
  private final UserData userData;

  public IssueLinkEventRecipientResolver(
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
  public IssueLinkEventRecipientsData resolve(final IssueLinkEventData issueLinkEventData) {
    final IssueEvent issueEventForSourceIssue =
        new IssueEvent(
            issueLinkEventData.getIssueLink().getSourceObject(),
            Map.of(),
            issueLinkEventData.getIssueLinkCreatorOrRemover(),
            EventType.ISSUE_UPDATED_ID);
    final Set<EventRecipient> notificationRecipientsForSourceIssue =
        resolve(issueEventForSourceIssue);

    final IssueEvent issueEventForDestinationIssue =
        new IssueEvent(
            issueLinkEventData.getIssueLink().getDestinationObject(),
            Map.of(),
            issueLinkEventData.getIssueLinkCreatorOrRemover(),
            EventType.ISSUE_UPDATED_ID);
    final Set<EventRecipient> notificationRecipientsForDestinationIssue =
        resolve(issueEventForDestinationIssue);

    resolveDuplicatedUserForLinkedIssues(
        notificationRecipientsForSourceIssue, notificationRecipientsForDestinationIssue);

    return IssueLinkEventRecipientsData.of(
        notificationRecipientsForSourceIssue,
        issueLinkEventData.getIssueLink().getSourceObject().getKey(),
        issueLinkEventData.getIssueLink().getSourceObject().getSummary(),
        notificationRecipientsForDestinationIssue,
        issueLinkEventData.getIssueLink().getDestinationObject().getKey(),
        issueLinkEventData.getIssueLink().getDestinationObject().getSummary(),
        issueLinkEventData.getIssueLinkCreatorOrRemover(),
        issueLinkEventData.getIssueLink().getIssueLinkType().getName(),
        issueLinkEventData.isLinkCreated());
  }

  private Set<EventRecipient> resolve(IssueEvent issueEventForSourceIssue) {
    try {
      return super.resolveRecipientByNotificationScheme(issueEventForSourceIssue).stream()
          .map(NotificationRecipient::getUser)
          .filter(
              user ->
                  userData.isLinkNotificationEnable(user)
                      && super.canSendEventToUser(user, issueEventForSourceIssue.getIssue()))
          .map(EventRecipient::of)
          .collect(Collectors.toSet());
    } catch (GenericEntityException e) {
      SentryClient.capture(e, Map.of("issueKey", issueEventForSourceIssue.getIssue().getKey()));
      return Collections.emptySet();
    }
  }

  private static void resolveDuplicatedUserForLinkedIssues(
      final Set<EventRecipient> notificationRecipientsForSourceIssue,
      final Set<EventRecipient> notificationRecipientsForDestinationIssue) {
    notificationRecipientsForDestinationIssue.removeAll(notificationRecipientsForSourceIssue);
  }
}
