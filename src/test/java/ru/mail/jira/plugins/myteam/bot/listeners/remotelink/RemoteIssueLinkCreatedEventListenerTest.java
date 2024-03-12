/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.remotelink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.event.issue.link.RemoteIssueLinkUICreateEvent;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkData;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkRecipientsData;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkToVKTeamSender;

@ExtendWith(MockitoExtension.class)
class RemoteIssueLinkCreatedEventListenerTest {
  @Mock
  @SuppressWarnings("NullAway")
  private JiraAuthenticationContext jiraAuthenticationContext;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private RemoteIssueLinkService remoteIssueLinkService;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private IssueService issueService;

  @Mock
  @SuppressWarnings("NullAway")
  RemoteIssueLinkEventRecipientResolver remoteIssueLinkEventRecipientResolver;

  @Mock
  @SuppressWarnings("NullAway")
  RemoteIssueLinkToVKTeamSender remoteIssueLinkToVKTeamSender;

  @InjectMocks
  @SuppressWarnings("NullAway")
  RemoteIssueLinkCreatedEventListener remoteIssueLinkCreatedEventListener;

  @Test
  void onEventUserNotLoggedIn() {
    // GIVEN
    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(false);

    // WHEN
    remoteIssueLinkCreatedEventListener.onEvent(
        new RemoteIssueLinkUICreateEvent(mock(RemoteIssueLink.class)));

    // THEN
    verify(remoteIssueLinkService, never())
        .getRemoteIssueLink(any(ApplicationUser.class), anyLong());
    verify(issueService, never()).getIssue(any(ApplicationUser.class), anyLong());
    verify(remoteIssueLinkEventRecipientResolver, never()).resolve(any(RemoteIssueLinkData.class));
    verify(remoteIssueLinkToVKTeamSender, never()).send(any(RemoteIssueLinkRecipientsData.class));
  }

  @Test
  void onEventRemoteIssueLinkSearchValidationResultNotValid() {
    // GIVEN
    long remoteIssueLinkId = 10000L;
    RemoteIssueLink remoteIssueLink = mock(RemoteIssueLink.class);
    when(remoteIssueLink.getId()).thenReturn(remoteIssueLinkId);

    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(linkCreator);

    RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkResult =
        mock(RemoteIssueLinkService.RemoteIssueLinkResult.class);
    when(remoteIssueLinkResult.isValid()).thenReturn(false);
    when(remoteIssueLinkService.getRemoteIssueLink(same(linkCreator), eq(remoteIssueLinkId)))
        .thenReturn(remoteIssueLinkResult);

    // WHEN
    remoteIssueLinkCreatedEventListener.onEvent(new RemoteIssueLinkUICreateEvent(remoteIssueLink));

    // THEN
    verify(issueService, never()).getIssue(any(ApplicationUser.class), anyLong());
    verify(remoteIssueLinkEventRecipientResolver, never()).resolve(any(RemoteIssueLinkData.class));
    verify(remoteIssueLinkToVKTeamSender, never()).send(any(RemoteIssueLinkRecipientsData.class));
  }

  @Test
  void onEventIssueSearchValidationResultNotValid() {
    // GIVEN
    long remoteIssueLinkId = 10000L;
    RemoteIssueLink remoteIssueLink = mock(RemoteIssueLink.class);
    when(remoteIssueLink.getId()).thenReturn(remoteIssueLinkId);
    long issueId = 10001L;
    when(remoteIssueLink.getIssueId()).thenReturn(issueId);

    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(linkCreator);

    RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkResult =
        mock(RemoteIssueLinkService.RemoteIssueLinkResult.class);
    when(remoteIssueLinkResult.isValid()).thenReturn(true);
    when(remoteIssueLinkResult.getRemoteIssueLink()).thenReturn(remoteIssueLink);
    when(remoteIssueLinkService.getRemoteIssueLink(same(linkCreator), eq(remoteIssueLinkId)))
        .thenReturn(remoteIssueLinkResult);

    IssueService.IssueResult issueResult = mock(IssueService.IssueResult.class);
    when(issueResult.isValid()).thenReturn(false);
    when(issueService.getIssue(same(linkCreator), eq(issueId))).thenReturn(issueResult);

    // WHEN
    remoteIssueLinkCreatedEventListener.onEvent(new RemoteIssueLinkUICreateEvent(remoteIssueLink));

    // THEN
    verify(remoteIssueLinkEventRecipientResolver, never()).resolve(any(RemoteIssueLinkData.class));
    verify(remoteIssueLinkToVKTeamSender, never()).send(any(RemoteIssueLinkRecipientsData.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "     ", "someLinkTitle"})
  void onEventSuccessWithEmptyRecipients(String linkTitle) {
    // GIVEN
    long remoteIssueLinkId = 10000L;
    RemoteIssueLink remoteIssueLink = mock(RemoteIssueLink.class);
    when(remoteIssueLink.getUrl()).thenReturn("someUrl");
    when(remoteIssueLink.getTitle()).thenReturn(linkTitle);
    when(remoteIssueLink.getId()).thenReturn(remoteIssueLinkId);
    long issueId = 10001L;
    when(remoteIssueLink.getIssueId()).thenReturn(issueId);

    when(jiraAuthenticationContext.isLoggedInUser()).thenReturn(true);
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(linkCreator);

    RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkResult =
        mock(RemoteIssueLinkService.RemoteIssueLinkResult.class);
    when(remoteIssueLinkResult.isValid()).thenReturn(true);
    when(remoteIssueLinkResult.getRemoteIssueLink()).thenReturn(remoteIssueLink);
    when(remoteIssueLinkService.getRemoteIssueLink(same(linkCreator), eq(remoteIssueLinkId)))
        .thenReturn(remoteIssueLinkResult);

    IssueService.IssueResult issueResult = mock(IssueService.IssueResult.class);
    when(issueResult.isValid()).thenReturn(true);
    MockIssue mockIssue = new MockIssue();
    mockIssue.setKey("KEY-123");
    when(issueResult.getIssue()).thenReturn(mockIssue);
    when(issueService.getIssue(same(linkCreator), eq(issueId))).thenReturn(issueResult);

    when(remoteIssueLinkEventRecipientResolver.resolve(any(RemoteIssueLinkData.class)))
        .thenReturn(Set.of(EventRecipient.of(linkCreator)));
    // WHEN
    remoteIssueLinkCreatedEventListener.onEvent(new RemoteIssueLinkUICreateEvent(remoteIssueLink));

    // THEN
    verify(remoteIssueLinkEventRecipientResolver).resolve(any(RemoteIssueLinkData.class));
    verify(remoteIssueLinkToVKTeamSender).send(any(RemoteIssueLinkRecipientsData.class));
  }
}
