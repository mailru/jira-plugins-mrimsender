/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;

@ExtendWith(MockitoExtension.class)
class JiraIssueLinkToChatMessageConverterTest {

  @Mock
  @SuppressWarnings("NullAway")
  private I18nResolver i18nResolver;

  @Mock
  @SuppressWarnings("NullAway")
  private MessageFormatter messageFormatter;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private JiraIssueLinkToChatMessageConverter jiraIssueLinkToChatMessageConverter;

  @Test
  void convertOnCreatedLink() {
    // GIVEN
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    IssueLinkEventToChatMessageData issueLinkEventToChatMessageData =
        new IssueLinkEventToChatMessageData(
            "SOURCE-123",
            "DESTINATION-123",
            "sourceIssueSummary",
            "destinationIssueSummary",
            eventCreator,
            "blocks",
            true);
    when(messageFormatter.formatUserToVKTeamsSysProfile(eventCreator)).thenReturn("some user");
    when(messageFormatter.createMarkdownIssueLink(eq("SOURCE-123")))
        .thenReturn("some link on SOURCE-123");
    when(messageFormatter.createMarkdownIssueLink(eq("DESTINATION-123")))
        .thenReturn("some link on DESTINATION-123");

    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.notification.issue.link.created"),
            eq("some user"),
            eq("blocks"),
            eq("some link on SOURCE-123"),
            eq("sourceIssueSummary"),
            eq("some link on DESTINATION-123"),
            eq("destinationIssueSummary")))
        .thenReturn("some message on created link");

    // WHEN
    String convert = jiraIssueLinkToChatMessageConverter.convert(issueLinkEventToChatMessageData);

    // THEN

    assertEquals("some message on created link", convert);
  }

  @Test
  void convertOnDeletedLink() {
    // GIVEN
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    IssueLinkEventToChatMessageData issueLinkEventToChatMessageData =
        new IssueLinkEventToChatMessageData(
            "SOURCE-123",
            "DESTINATION-123",
            "sourceIssueSummary",
            "destinationIssueSummary",
            eventCreator,
            "blocks",
            false);
    when(messageFormatter.formatUserToVKTeamsSysProfile(eventCreator)).thenReturn("some user");
    when(messageFormatter.createMarkdownIssueLink(eq("SOURCE-123")))
        .thenReturn("some link on SOURCE-123");
    when(messageFormatter.createMarkdownIssueLink(eq("DESTINATION-123")))
        .thenReturn("some link on DESTINATION-123");

    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.notification.issue.link.deleted"),
            eq("some user"),
            eq("blocks"),
            eq("some link on SOURCE-123"),
            eq("sourceIssueSummary"),
            eq("some link on DESTINATION-123"),
            eq("destinationIssueSummary")))
        .thenReturn("some message on deleted link");

    // WHEN
    String convert = jiraIssueLinkToChatMessageConverter.convert(issueLinkEventToChatMessageData);

    // THEN

    assertEquals("some message on deleted link", convert);
  }
}
