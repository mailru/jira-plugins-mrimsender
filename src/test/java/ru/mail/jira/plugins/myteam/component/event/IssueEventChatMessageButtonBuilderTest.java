/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.sal.api.message.I18nResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

@ExtendWith(MockitoExtension.class)
class IssueEventChatMessageButtonBuilderTest {
  @Mock
  @SuppressWarnings("NullAway")
  private I18nResolver i18nResolver;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder;

  @Test
  void build() {
    // GIVEN
    String issueKey = "KEY-123";
    when(i18nResolver.getRawText(
            eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text")))
        .thenReturn("Comment");
    when(i18nResolver.getRawText(
            eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text")))
        .thenReturn("Show comment");
    when(i18nResolver.getRawText(
            eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.assign.text")))
        .thenReturn("Assign");
    when(i18nResolver.getRawText(
            eq("ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.title")))
        .thenReturn("Transition");
    when(i18nResolver.getRawText(
            eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.quickViewButton.text")))
        .thenReturn("Quick View");
    when(i18nResolver.getText(
            eq("ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text")))
        .thenReturn("Main Menu");

    // WHEN
    List<List<InlineKeyboardMarkupButton>> buttons =
        issueEventChatMessageButtonBuilder.build(issueKey);

    // THEN
    assertNotNull(buttons);
    assertFalse(buttons.isEmpty());
    assertEquals(3, buttons.size());

    verify(i18nResolver)
        .getRawText(eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"));
    verify(i18nResolver)
        .getRawText(
            eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text"));
    verify(i18nResolver)
        .getRawText(eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.assign.text"));
    verify(i18nResolver)
        .getRawText(
            eq("ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.title"));
    verify(i18nResolver)
        .getRawText(eq("ru.mail.jira.plugins.myteam.mrimsenderEventListener.quickViewButton.text"));
    verify(i18nResolver)
        .getText(eq("ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"));
  }
}
