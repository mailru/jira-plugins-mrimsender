/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

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
class RemoteIssueLinkToVKTeamSenderTest {
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
  private RemoteIssueLinkToChatMessageConverter remoteIssueLinkToChatMessageConverter;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private RemoteIssueLinkToVKTeamSender remoteIssueLinkToVKTeamSender;

  @Test
  void sendWhenRecipientsEmpty() {
    // GIVEN
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    RemoteIssueLinkRecipientsData data =
        RemoteIssueLinkRecipientsData.of(
            "linkTitle", "someUrl", "KEY-123", "issueSummary", linkCreator, Set.of());

    // WHEN
    remoteIssueLinkToVKTeamSender.send(data);

    // THEN
    verify(userData, never()).isEnabled(any(ApplicationUser.class));
    verify(jiraAuthenticationContext, never()).getLoggedInUser();
    verify(jiraAuthenticationContext, never()).setLoggedInUser(any(ApplicationUser.class));
    verify(myteamEventsListener, never()).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder, never()).build(any(String.class));
    verify(remoteIssueLinkToChatMessageConverter, never())
        .convert(any(RemoteIssueLinkRecipientsData.class));
  }

  @Test
  void sendSuccess() {
    // GIVEN
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    when(linkCreator.isActive()).thenReturn(true);
    when(linkCreator.getEmailAddress()).thenReturn("admin@example.org");
    RemoteIssueLinkRecipientsData data =
        RemoteIssueLinkRecipientsData.of(
            "linkTitle",
            "someUrl",
            "KEY-123",
            "issueSummary",
            linkCreator,
            Set.of(EventRecipient.of(linkCreator)));

    when(userData.isEnabled(same(linkCreator))).thenReturn(true);
    when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(linkCreator);
    when(remoteIssueLinkToChatMessageConverter.convert(same(data))).thenReturn("someMessage");

    // WHEN
    remoteIssueLinkToVKTeamSender.send(data);

    // THEN
    verify(userData).isEnabled(same(linkCreator));
    verify(jiraAuthenticationContext).getLoggedInUser();
    verify(jiraAuthenticationContext, times(2)).setLoggedInUser(same(linkCreator));
    verify(myteamEventsListener).publishEvent(any(JiraNotifyEvent.class));
    verify(issueEventChatMessageButtonBuilder).build(eq("KEY-123"));
    verify(remoteIssueLinkToChatMessageConverter).convert(same(data));
  }
}
