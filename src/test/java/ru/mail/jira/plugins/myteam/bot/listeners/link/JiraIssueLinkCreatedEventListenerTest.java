/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.link;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventData;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventRecipientsData;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventToVKTeamSender;

@ExtendWith(MockitoExtension.class)
class JiraIssueLinkCreatedEventListenerTest {

  @Mock
  @SuppressWarnings("NullAway")
  private JiraAuthenticationContext jiraAuthenticationContext;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private IssueLinkEventRecipientResolver issueLinkEventRecipientResolver;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender;

  @InjectMocks
  @SuppressWarnings("NullAway")
  JiraIssueLinkCreatedEventListener jiraIssueLinkCreatedEventListener;

  @Test
  void onEventWhenUserNotLoggedInSystem() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(false);

    // WHEN
    jiraIssueLinkCreatedEventListener.onEvent(
        new IssueLinkCreatedEvent(mock(IssueLink.class), Instant.now()));

    // THEN
    verify(jiraAuthenticationContext).isLoggedInUser();
    verify(issueLinkEventRecipientResolver, never()).resolve(any(IssueLinkEventData.class));
    verify(issueLinkEventToVKTeamSender, never()).send(any(IssueLinkEventRecipientsData.class));
  }

  @Test
  void onEventSuccess() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);
    IssueLinkCreatedEvent issueLinkCreatedEvent =
        new IssueLinkCreatedEvent(mock(IssueLink.class), Instant.now());
    IssueLinkEventRecipientsData recipientsData =
        IssueLinkEventRecipientsData.of(
            Set.of(EventRecipient.of(loggedInUser)),
            "key1",
            "sourceIssueSummary",
            Collections.emptySet(),
            "key2",
            "destinationIssueSummary",
            loggedInUser,
            "blocks",
            true);
    when(issueLinkEventRecipientResolver.resolve(any(IssueLinkEventData.class)))
        .thenReturn(recipientsData);

    // WHEN
    jiraIssueLinkCreatedEventListener.onEvent(issueLinkCreatedEvent);

    // THEN
    verify(jiraAuthenticationContext).isLoggedInUser();
    verify(issueLinkEventRecipientResolver).resolve(any(IssueLinkEventData.class));
    verify(issueLinkEventToVKTeamSender).send(same(recipientsData));
  }

  @Test
  void onEventSendError() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);

    IssueLink issueLink = mock(IssueLink.class);
    IssueLinkType issueLinkType = mock(IssueLinkType.class);
    when(issueLinkType.getId()).thenReturn(1L);
    when(issueLinkType.getName()).thenReturn("mockLinkTypeName");
    when(issueLink.getIssueLinkType()).thenReturn(issueLinkType);

    Issue sourceIssue = mock(Issue.class);
    when(sourceIssue.getKey()).thenReturn("key1");
    when(issueLink.getSourceObject()).thenReturn(sourceIssue);
    Issue destinationIssue = mock(Issue.class);
    when(destinationIssue.getKey()).thenReturn("key2");
    when(issueLink.getDestinationObject()).thenReturn(destinationIssue);

    IssueLinkCreatedEvent issueLinkCreatedEvent =
        new IssueLinkCreatedEvent(issueLink, Instant.now());
    IssueLinkEventRecipientsData recipientsData =
        IssueLinkEventRecipientsData.of(
            Set.of(EventRecipient.of(loggedInUser)),
            "key1",
            "sourceIssueSummary",
            Collections.emptySet(),
            "key2",
            "destinationIssueSummary",
            loggedInUser,
            "blocks",
            true);

    when(issueLinkEventRecipientResolver.resolve(any(IssueLinkEventData.class)))
        .thenReturn(recipientsData);
    doThrow(new RuntimeException()).when(issueLinkEventToVKTeamSender).send(same(recipientsData));

    // WHEN
    jiraIssueLinkCreatedEventListener.onEvent(issueLinkCreatedEvent);

    // THEN
    verify(jiraAuthenticationContext).isLoggedInUser();
    verify(issueLinkEventRecipientResolver).resolve(any(IssueLinkEventData.class));
    verify(issueLinkEventToVKTeamSender).send(same(recipientsData));

    verify(issueLink, times(2)).getIssueLinkType();
    verify(issueLinkType).getId();
    verify(issueLinkType).getName();
    verify(issueLink).getSourceObject();
    verify(sourceIssue).getKey();
    verify(issueLink).getDestinationObject();
    verify(destinationIssue).getKey();
  }
}
