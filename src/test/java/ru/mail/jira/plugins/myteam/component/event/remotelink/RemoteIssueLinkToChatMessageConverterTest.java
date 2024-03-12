/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;

@ExtendWith(MockitoExtension.class)
class RemoteIssueLinkToChatMessageConverterTest {

  @Mock
  @SuppressWarnings("NullAway")
  private I18nResolver i18nResolver;

  @Mock
  @SuppressWarnings("NullAway")
  private MessageFormatter messageFormatter;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private RemoteIssueLinkToChatMessageConverter remoteIssueLinkToChatMessageConverter;

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void convertWhenTitleEmptyString(final String linkTitle) {
    // GIVEN
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    RemoteIssueLinkRecipientsData data =
        RemoteIssueLinkRecipientsData.of(
            linkTitle, "someUrl", "KEY-123", linkCreator, Set.of(EventRecipient.of(linkCreator)));
    when(messageFormatter.createMarkdownIssueLink(eq("KEY-123"))).thenReturn("some url on KEY-123");
    when(messageFormatter.formatUserToVKTeamsSysProfile(same(linkCreator)))
        .thenReturn("some url on link creator");
    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.notification.remote.issue.link.created"),
            eq("some url on link creator"),
            eq("someUrl"),
            eq("some url on KEY-123")))
        .thenReturn("link creator create remote link someUrl in [KEY-123|someUrlOnKey-123]");

    // WHEN
    String result = remoteIssueLinkToChatMessageConverter.convert(data);

    // THEN
    assertEquals("link creator create remote link someUrl in [KEY-123|someUrlOnKey-123]", result);
  }

  void convertWhenTitleNotEmptyString() {
    // GIVEN
    ApplicationUser linkCreator = mock(ApplicationUser.class);
    RemoteIssueLinkRecipientsData data =
        RemoteIssueLinkRecipientsData.of(
            "someTitle", "someUrl", "KEY-123", linkCreator, Set.of(EventRecipient.of(linkCreator)));
    when(messageFormatter.createMarkdownIssueLink(eq("KEY-123"))).thenReturn("some url on KEY-123");
    when(messageFormatter.formatUserToVKTeamsSysProfile(same(linkCreator)))
        .thenReturn("some url on link creator");
    when(messageFormatter.markdownTextLink(eq("someTitle"), eq("someUrl")))
        .thenReturn("[someTitle|someUrl]");

    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.notification.remote.issue.link.created"),
            eq("some url on link creator"),
            eq("[someTitle|someUrl]"),
            eq("some url on KEY-123")))
        .thenReturn(
            "link creator create remote link [someTitle|someUrl] in [KEY-123|someUrlOnKey-123]");

    // WHEN
    String result = remoteIssueLinkToChatMessageConverter.convert(data);

    // THEN
    assertEquals("link creator create remote link someUrl in [KEY-123|someUrlOnKey-123]", result);
  }
}
