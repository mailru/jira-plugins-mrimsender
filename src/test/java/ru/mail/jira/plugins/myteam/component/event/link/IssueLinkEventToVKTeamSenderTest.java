/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.IssueEventChatMessageButtonBuilder;

@ExtendWith(MockitoExtension.class)
class IssueLinkEventToVKTeamSenderTest {
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
  private JiraIssueLinkToChatMessageConverter jiraIssueLinkToChatMessageConverter;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender;

  @Test
  void sendWhenSourceAndDestinationIssueNotificationRecipientAreEmpty() {
    // GIVEN
    ApplicationUser issueLinkCreator = mock(ApplicationUser.class);
    IssueLinkEventRecipientsData issueLinkEventRecipientsData =
        IssueLinkEventRecipientsData.of(
            Set.of(),
            "SOURCE-123",
            "sourceIssueSummary",
            Set.of(),
            "DESTINATION-123",
            "destinationIssueSummary",
            issueLinkCreator,
            "blocks",
            true);

    // WHEN
    issueLinkEventToVKTeamSender.send(issueLinkEventRecipientsData);

    // THEN
    verify(userData, never()).isEnabled(any(ApplicationUser.class));
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueLinkToChatMessageConverter, never())
        .convert(any(IssueLinkEventToChatMessageData.class));
  }

  @Test
  void sendWhenSourceIssueAndDestinationNotificationRecipientsAreNotEmpty() {
    // GIVEN
    ApplicationUser issueLinkCreator = mock(ApplicationUser.class);
    ApplicationUser recipientOfSourceIssueLink = mock(ApplicationUser.class);
    ApplicationUser recipientOfDestinationIssueLink = mock(ApplicationUser.class);
    when(recipientOfSourceIssueLink.isActive()).thenReturn(true);
    when(recipientOfSourceIssueLink.getEmailAddress()).thenReturn("admin@example.org");

    when(recipientOfDestinationIssueLink.isActive()).thenReturn(true);
    when(recipientOfDestinationIssueLink.getEmailAddress()).thenReturn("admin1@example.org");
    IssueLinkEventRecipientsData issueLinkEventRecipientsData =
        IssueLinkEventRecipientsData.of(
            Set.of(EventRecipient.of(recipientOfSourceIssueLink)),
            "SOURCE-123",
            "sourceIssueSummary",
            Set.of(EventRecipient.of(recipientOfDestinationIssueLink)),
            "DESTINATION-123",
            "destinationIssueSummary",
            issueLinkCreator,
            "blocks",
            true);

    when(userData.isEnabled(same(recipientOfSourceIssueLink))).thenReturn(true);
    when(userData.isEnabled(same(recipientOfDestinationIssueLink))).thenReturn(true);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(issueLinkCreator);
    when(jiraIssueLinkToChatMessageConverter.convert(any(IssueLinkEventToChatMessageData.class)))
        .thenReturn("someMessage1");
    when(jiraIssueLinkToChatMessageConverter.convert(any(IssueLinkEventToChatMessageData.class)))
        .thenReturn("someMessage2");

    // WHEN
    issueLinkEventToVKTeamSender.send(issueLinkEventRecipientsData);

    // THEN
    verify(userData).isEnabled(same(recipientOfSourceIssueLink));
    verify(userData).isEnabled(same(recipientOfDestinationIssueLink));
    verify(jiraAuthenticationContext, times(2)).getLoggedInUser();
    verify(jiraAuthenticationContext).setLoggedInUser(same(recipientOfDestinationIssueLink));
    verify(jiraAuthenticationContext).setLoggedInUser(same(recipientOfSourceIssueLink));
    verify(jiraAuthenticationContext, times(2)).setLoggedInUser(same(issueLinkCreator));
    verify(myteamEventsListener, times(2)).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(jiraIssueLinkToChatMessageConverter, times(2))
        .convert(any(IssueLinkEventToChatMessageData.class));
  }
}
