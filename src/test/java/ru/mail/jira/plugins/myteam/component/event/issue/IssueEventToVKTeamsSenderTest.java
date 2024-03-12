/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.issue;

import static org.mockito.Mockito.*;

import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.IssueEventRecipient;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.JiraIssueEventToChatMessageConverter;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.IssueEventChatMessageButtonBuilder;

@ExtendWith(MockitoExtension.class)
class IssueEventToVKTeamsSenderTest {
  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private JiraAuthenticationContext jiraAuthenticationContext;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private MyteamEventsListener myteamEventsListener;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private UserData userData;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder;

  @Mock(lenient = true)
  @SuppressWarnings("NullAway")
  private JiraIssueEventToChatMessageConverter jiraIssueEventToChatMessageConverter;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private IssueEventToVKTeamsSender issueEventToVKTeamsSender;

  @Test
  void sendWhenRecipientsIsEmpty() {
    // GIVEN
    IssueEventData issueEventData = IssueEventData.of(Set.of(), mock(IssueEvent.class));

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData, never()).isEnabled(any(ApplicationUser.class));
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter, never())
        .convert(any(IssueEventToChatMessageData.class));
  }

  @Test
  void sendWhenRecipientsNotActive() {
    // GIVEN
    ApplicationUser notActiveRecipient = mock(ApplicationUser.class);
    when(notActiveRecipient.isActive()).thenReturn(false);

    IssueEvent issueEvent = mock(IssueEvent.class);
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("KEY-123");
    when(issueEvent.getIssue()).thenReturn(issue);

    IssueEventData issueEventData =
        IssueEventData.of(Set.of(IssueEventRecipient.of(notActiveRecipient, false)), issueEvent);

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData, never()).isEnabled(any(ApplicationUser.class));
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter, never())
        .convert(any(IssueEventToChatMessageData.class));
  }

  @Test
  void sendWhenRecipientsNotEnabledToReceiveNotification() {
    // GIVEN
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.isActive()).thenReturn(true);

    IssueEvent issueEvent = mock(IssueEvent.class);
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("KEY-123");
    when(issueEvent.getIssue()).thenReturn(issue);

    IssueEventData issueEventData =
        IssueEventData.of(Set.of(IssueEventRecipient.of(recipient, false)), issueEvent);

    when(userData.isEnabled(same(recipient))).thenReturn(false);

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData).isEnabled(same(recipient));
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter, never())
        .convert(any(IssueEventToChatMessageData.class));
  }

  @ParameterizedTest
  @EmptySource
  void sendWhenRecipientsHasEmptyEmailAddress(String emailAddress) {
    // GIVEN
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.isActive()).thenReturn(true);
    when(recipient.getEmailAddress()).thenReturn(emailAddress);

    IssueEvent issueEvent = mock(IssueEvent.class);
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("KEY-123");
    when(issueEvent.getIssue()).thenReturn(issue);

    IssueEventData issueEventData =
        IssueEventData.of(Set.of(IssueEventRecipient.of(recipient, false)), issueEvent);

    when(userData.isEnabled(same(recipient))).thenReturn(true);

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData).isEnabled(same(recipient));
    verify(recipient, times(1)).getEmailAddress();
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter, never())
        .convert(any(IssueEventToChatMessageData.class));
  }

  @Test
  void sendWhenMessageIsEmptyString() {
    // GIVEN
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.isActive()).thenReturn(true);
    when(recipient.getEmailAddress()).thenReturn("admin@example.org");

    IssueEvent issueEvent = mock(IssueEvent.class);
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("KEY-123");
    when(issueEvent.getIssue()).thenReturn(issue);

    IssueEventData issueEventData =
        IssueEventData.of(Set.of(IssueEventRecipient.of(recipient, false)), issueEvent);

    when(userData.isEnabled(same(recipient))).thenReturn(true);

    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);
    when(jiraIssueEventToChatMessageConverter.convert(any(IssueEventToChatMessageData.class)))
        .thenReturn("");

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData).isEnabled(same(recipient));
    verify(recipient, times(1)).getEmailAddress();
    verify(jiraAuthenticationContext).getLoggedInUser();
    verify(jiraAuthenticationContext).setLoggedInUser(recipient);
    verify(jiraAuthenticationContext).setLoggedInUser(loggedInUser);
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter).convert(any(IssueEventToChatMessageData.class));
  }

  @Test
  void sendSuccess() {
    // GIVEN
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.isActive()).thenReturn(true);
    when(recipient.getEmailAddress()).thenReturn("admin@example.org");

    IssueEvent issueEvent = mock(IssueEvent.class);
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("KEY-123");
    when(issueEvent.getIssue()).thenReturn(issue);

    IssueEventData issueEventData =
        IssueEventData.of(Set.of(IssueEventRecipient.of(recipient, false)), issueEvent);

    when(userData.isEnabled(same(recipient))).thenReturn(true);
    ApplicationUser loggedInUser = mock(ApplicationUser.class);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(loggedInUser);
    when(jiraIssueEventToChatMessageConverter.convert(any(IssueEventToChatMessageData.class)))
        .thenReturn("someMessage");

    // WHEN
    issueEventToVKTeamsSender.send(issueEventData);

    // THEN
    verify(userData).isEnabled(same(recipient));
    verify(jiraAuthenticationContext).getLoggedInUser();
    verify(jiraAuthenticationContext).setLoggedInUser(same(recipient));
    verify(jiraAuthenticationContext).setLoggedInUser(same(loggedInUser));
    verify(myteamEventsListener).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder).build(any(String.class));
    verify(jiraIssueEventToChatMessageConverter).convert(any(IssueEventToChatMessageData.class));
    verify(recipient, times(2)).getEmailAddress();
  }
}
