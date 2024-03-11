/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.issue;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.notification.*;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.UserMentionService;
import ru.mail.jira.plugins.myteam.component.event.AbstractRecipientResolver;

@Component
public class IssueEventRecipientResolver
    extends AbstractRecipientResolver<IssueEvent, Set<IssueEventRecipient>> {
  private final UserMentionService userMentionService;

  public IssueEventRecipientResolver(
      @ComponentImport final NotificationFilterManager notificationFilterManager,
      @ComponentImport final NotificationSchemeManager notificationSchemeManager,
      @ComponentImport final PermissionManager permissionManager,
      @ComponentImport final ProjectRoleManager projectRoleManager,
      @ComponentImport final GroupManager groupManager,
      final UserMentionService userMentionService) {
    super(
        notificationFilterManager,
        notificationSchemeManager,
        permissionManager,
        projectRoleManager,
        groupManager);
    this.userMentionService = userMentionService;
  }

  @Override
  public Set<IssueEventRecipient> resolve(final IssueEvent issueEvent) {
    try {
      final Set<ApplicationUser> mentionedPossibleRecipients =
          resolvePossibleRecipientsMentionedInIssueEvent(issueEvent);

      final Set<NotificationRecipient> notificationRecipients =
          super.resolveRecipientByNotificationScheme(issueEvent);
      return Collections.unmodifiableSet(
          resolveRecipientsFilteredByPermissionsAndMentions(
              issueEvent, mentionedPossibleRecipients, notificationRecipients));
    } catch (Exception e) {
      SentryClient.capture(e, Map.of("issueKey", issueEvent.getIssue().getKey()));
      return Collections.emptySet();
    }
  }

  @NotNull
  private Set<ApplicationUser> resolvePossibleRecipientsMentionedInIssueEvent(
      @NotNull final IssueEvent issueEvent) {
    Long eventTypeId = issueEvent.getEventTypeId();
    Set<ApplicationUser> mentionedUsers;
    if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId)) {
      mentionedUsers =
          userMentionService.getMentionedUsersInDescription(issueEvent.getIssue(), true);
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
      mentionedUsers = userMentionService.getMentionedUserInComment(issueEvent.getComment());
    } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      Object origCommentObject =
          issueEvent.getParams().get(CommentManager.EVENT_ORIGINAL_COMMENT_PARAMETER);
      if (origCommentObject instanceof Comment) {
        mentionedUsers =
            userMentionService.getMentionedUserInEditedComment(
                issueEvent.getComment(), (Comment) origCommentObject);
      } else {
        mentionedUsers = Collections.emptySet();
      }
    } else {
      mentionedUsers = Collections.emptySet();
    }
    return mentionedUsers;
  }

  @NotNull
  private Set<IssueEventRecipient> resolveRecipientsFilteredByPermissionsAndMentions(
      @NotNull final IssueEvent issueEvent,
      @NotNull final Set<ApplicationUser> mentionedPossibleRecipients,
      @NotNull final Set<NotificationRecipient> notificationRecipients) {
    final Map<String, ApplicationUser> mentionedPossibleRecipientsMap =
        mentionedPossibleRecipients.stream()
            .filter(mentionedUser -> super.canSendEventToUser(mentionedUser, issueEvent))
            .collect(Collectors.toMap(ApplicationUser::getKey, Function.identity()));

    final Set<ApplicationUser> filteredRecipientsByPermissions =
        notificationRecipients.stream()
            .map(NotificationRecipient::getUser)
            .filter(user -> super.canSendEventToUser(user, issueEvent))
            .collect(Collectors.toSet());

    final Set<IssueEventRecipient> recipients = new HashSet<>();
    for (final ApplicationUser recipient : filteredRecipientsByPermissions) {
      final ApplicationUser mentionedUser =
          mentionedPossibleRecipientsMap.remove(recipient.getKey());
      final boolean mentioned = mentionedUser != null;
      recipients.add(IssueEventRecipient.of(recipient, mentioned));
    }

    recipients.addAll(
        mentionedPossibleRecipientsMap.values().stream()
            .map(lostMentionRecipient -> IssueEventRecipient.of(lostMentionRecipient, true))
            .collect(Collectors.toSet()));
    return recipients;
  }
}
