/* (C)2021 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SuppressWarnings({"MockNotUsedInProduction", "UnusedVariable"})
@ExtendWith(MockitoExtension.class)
@MockitoSettings
class JiraEventToChatMessageConverterOnCommentCreatedOrEditedEventTest {
  @Mock
  @SuppressWarnings("NullAway")
  private MessageFormatter messageFormatter;

  @Mock
  @SuppressWarnings("NullAway")
  private JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter;

  @Mock
  @SuppressWarnings("NullAway")
  private DiffFieldChatMessageGenerator diffFieldChatMessageGenerator;

  @Mock
  @SuppressWarnings("NullAway")
  private AttachmentManager attachmentManager;

  @Mock
  @SuppressWarnings("NullAway")
  private ApplicationProperties applicationProperties;

  @Mock
  @SuppressWarnings("NullAway")
  private I18nResolver i18nResolver;

  @Mock
  @SuppressWarnings("NullAway")
  private I18nHelper i18nHelper;

  @Mock
  @SuppressWarnings("NullAway")
  private FieldManager fieldManager;

  @Mock
  @SuppressWarnings("NullAway")
  private UserManager userManager;

  @Mock
  @SuppressWarnings("NullAway")
  private PluginMentionService pluginMentionService;

  @InjectMocks
  @SuppressWarnings("NullAway")
  private JiraEventToChatMessageConverter jiraEventToChatMessageConverter;

  @Test
  void onCommentCreatedEventIfUserIsMentioned() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("someKey");
    when(issue.getSummary()).thenReturn("someSummary");
    when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL)))
        .thenReturn("http://localhost:8080");
    Comment comment = mock(Comment.class);
    when(comment.getId()).thenReturn(1L);
    when(messageFormatter.getIssueLink(eq("someKey")))
        .thenReturn("http://localhost:8080/browse/someKey");

    IssueEvent commentCreatedEventWithMentionInCommentBody = mock(IssueEvent.class);
    when(commentCreatedEventWithMentionInCommentBody.getParams())
        .thenReturn(Collections.emptyMap());
    when(commentCreatedEventWithMentionInCommentBody.getIssue())
        .thenReturn(issue)
        .thenReturn(issue);
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(commentCreatedEventWithMentionInCommentBody.getUser()).thenReturn(eventCreator);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENTED_ID);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENTED_ID);

    when(pluginMentionService.checkMentionUserInComment(
            eq(recipient), eq(comment), nullable(Comment.class)))
        .thenReturn(true);

    String createdCommentBody = "Comment with mention [~userToNotify]";
    String convertedCreatedCommentBody =
        "Comment with mention ±@\\±[notifiedUser@example\\.org\\±]";
    when(comment.getBody()).thenReturn(createdCommentBody);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
            eq(createdCommentBody), eq(true)))
        .thenReturn(convertedCreatedCommentBody);

    when(messageFormatter.formatUser(eq(eventCreator), eq("common.words.anonymous"), eq(true)))
        .thenReturn("±@\\±[eventCreator@example\\.org\\±]");

    String expectedResult =
        "±@\\±[eventCreator@example\\.org\\±] упомянул в вас [комментарии](http://localhost:8080/browse/someKey?focusedCommentId=1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1)\n\n"
            + convertedCreatedCommentBody;
    when(i18nResolver.getText(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedResult);

    // WHEN
    String messageText =
        jiraEventToChatMessageConverter.formatEventWithDiff(
            recipient, commentCreatedEventWithMentionInCommentBody);

    // THEN
    assertNotNull(messageText);
    assertFalse(messageText.isEmpty());
    assertEquals(expectedResult, messageText);

    verify(commentCreatedEventWithMentionInCommentBody, times(2)).getEventTypeId();
    verify(jiraMarkdownToChatMarkdownConverter)
        .makeMyteamMarkdownFromJira(eq(createdCommentBody), eq(true));
    verify(messageFormatter).getIssueLink(eq("someKey"));
    verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
  }

  @Test
  void onCommentCreatedEventIfUserIsNotMentioned() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("someKey");
    when(issue.getSummary()).thenReturn("someSummary");
    when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL)))
        .thenReturn("http://localhost:8080");
    Comment comment = mock(Comment.class);
    when(comment.getId()).thenReturn(1L);
    when(messageFormatter.getIssueLink(eq("someKey")))
        .thenReturn("http://localhost:8080/browse/someKey");

    IssueEvent commentCreatedEventWithMentionInCommentBody = mock(IssueEvent.class);
    when(commentCreatedEventWithMentionInCommentBody.getParams())
        .thenReturn(Collections.emptyMap());
    when(commentCreatedEventWithMentionInCommentBody.getIssue())
        .thenReturn(issue)
        .thenReturn(issue);
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(commentCreatedEventWithMentionInCommentBody.getUser()).thenReturn(eventCreator);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENTED_ID);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENTED_ID);

    String createdCommentBody = "Comment with mention [~userToNotify]";
    String convertedCreatedCommentBody =
        "Comment with mention ±@\\±[notifiedUser@example\\.org\\±]";
    when(comment.getBody()).thenReturn(createdCommentBody);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
            eq(createdCommentBody), eq(true)))
        .thenReturn(convertedCreatedCommentBody);

    when(messageFormatter.formatUser(eq(eventCreator), eq("common.words.anonymous"), eq(true)))
        .thenReturn("±@\\±[eventCreator@example\\.org\\±]");

    String expectedResult =
        "±@\\±[eventCreator@example\\.org\\±] упомянул в вас [комментарии](http://localhost:8080/browse/someKey?focusedCommentId=1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1)\n\n"
            + convertedCreatedCommentBody;
    when(i18nResolver.getText(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedResult);

    when(pluginMentionService.checkMentionUserInComment(
            eq(recipient), eq(comment), nullable(Comment.class)))
        .thenReturn(false);

    // WHEN
    String messageText =
        jiraEventToChatMessageConverter.formatEventWithDiff(
            recipient, commentCreatedEventWithMentionInCommentBody);

    // THEN
    assertNotNull(messageText);
    assertFalse(messageText.isEmpty());
    assertEquals(expectedResult, messageText);

    verify(commentCreatedEventWithMentionInCommentBody, times(1)).getEventTypeId();
    verify(jiraMarkdownToChatMarkdownConverter)
        .makeMyteamMarkdownFromJira(eq(createdCommentBody), eq(true));
    verify(messageFormatter).getIssueLink(eq("someKey"));
    verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
  }

  @Test
  void onCommentEditedEventIfUserIsMentioned() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("someKey");
    when(issue.getSummary()).thenReturn("someSummary");
    when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL)))
        .thenReturn("http://localhost:8080");
    Comment comment = mock(Comment.class);
    when(comment.getId()).thenReturn(1L);
    when(messageFormatter.getIssueLink(eq("someKey")))
        .thenReturn("http://localhost:8080/browse/someKey");

    IssueEvent commentCreatedEventWithMentionInCommentBody = mock(IssueEvent.class);
    Comment originalComment = mock(Comment.class);
    when(commentCreatedEventWithMentionInCommentBody.getParams())
        .thenReturn(Map.of("originalcomment", originalComment));
    when(commentCreatedEventWithMentionInCommentBody.getIssue())
        .thenReturn(issue)
        .thenReturn(issue);
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(commentCreatedEventWithMentionInCommentBody.getUser()).thenReturn(eventCreator);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENT_EDITED_ID);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENT_EDITED_ID);

    when(pluginMentionService.checkMentionUserInComment(
            eq(recipient), eq(comment), eq(originalComment)))
        .thenReturn(true);

    String createdCommentBody = "Comment with mention [~userToNotify]";
    String convertedCreatedCommentBody =
        "Comment with mention ±@\\±[notifiedUser@example\\.org\\±]";
    when(comment.getBody()).thenReturn(createdCommentBody);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
            eq(createdCommentBody), eq(true)))
        .thenReturn(convertedCreatedCommentBody);

    when(messageFormatter.formatUser(eq(eventCreator), eq("common.words.anonymous"), eq(true)))
        .thenReturn("±@\\±[eventCreator@example\\.org\\±]");

    String expectedResult =
        "±@\\±[eventCreator@example\\.org\\±] упомянул в вас [комментарии](http://localhost:8080/browse/someKey?focusedCommentId=1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1)\n\n"
            + convertedCreatedCommentBody;
    when(i18nResolver.getText(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedResult);

    // WHEN
    String messageText =
        jiraEventToChatMessageConverter.formatEventWithDiff(
            recipient, commentCreatedEventWithMentionInCommentBody);

    // THEN
    assertNotNull(messageText);
    assertFalse(messageText.isEmpty());
    assertEquals(expectedResult, messageText);

    verify(commentCreatedEventWithMentionInCommentBody, times(2)).getEventTypeId();
    verify(jiraMarkdownToChatMarkdownConverter)
        .makeMyteamMarkdownFromJira(eq(createdCommentBody), eq(true));
    verify(messageFormatter).getIssueLink(eq("someKey"));
    verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
  }

  @Test
  void onCommentEditedEventIfUserIsNotMentioned() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getKey()).thenReturn("someKey");
    when(issue.getSummary()).thenReturn("someSummary");
    when(applicationProperties.getString(eq(APKeys.JIRA_BASEURL)))
        .thenReturn("http://localhost:8080");
    Comment comment = mock(Comment.class);
    when(comment.getId()).thenReturn(1L);
    when(messageFormatter.getIssueLink(eq("someKey")))
        .thenReturn("http://localhost:8080/browse/someKey");

    IssueEvent commentCreatedEventWithMentionInCommentBody = mock(IssueEvent.class);
    Comment originalComment = mock(Comment.class);
    when(commentCreatedEventWithMentionInCommentBody.getParams())
        .thenReturn(Map.of("originalcomment", originalComment));
    when(commentCreatedEventWithMentionInCommentBody.getIssue())
        .thenReturn(issue)
        .thenReturn(issue);
    ApplicationUser eventCreator = mock(ApplicationUser.class);
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(commentCreatedEventWithMentionInCommentBody.getUser()).thenReturn(eventCreator);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENT_EDITED_ID);
    when(commentCreatedEventWithMentionInCommentBody.getEventTypeId())
        .thenReturn(EventType.ISSUE_COMMENT_EDITED_ID);

    String createdCommentBody = "Comment with mention [~userToNotify]";
    String convertedCreatedCommentBody =
        "Comment with mention ±@\\±[notifiedUser@example\\.org\\±]";
    when(comment.getBody()).thenReturn(createdCommentBody);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(commentCreatedEventWithMentionInCommentBody.getComment()).thenReturn(comment);
    when(jiraMarkdownToChatMarkdownConverter.makeMyteamMarkdownFromJira(
            eq(createdCommentBody), eq(true)))
        .thenReturn(convertedCreatedCommentBody);

    when(messageFormatter.formatUser(eq(eventCreator), eq("common.words.anonymous"), eq(true)))
        .thenReturn("±@\\±[eventCreator@example\\.org\\±]");

    String expectedResult =
        "±@\\±[eventCreator@example\\.org\\±] упомянул в вас [комментарии](http://localhost:8080/browse/someKey?focusedCommentId=1&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-1)\n\n"
            + convertedCreatedCommentBody;
    when(i18nResolver.getText(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(expectedResult);

    when(pluginMentionService.checkMentionUserInComment(
            eq(recipient), eq(comment), eq(originalComment)))
        .thenReturn(false);

    // WHEN
    String messageText =
        jiraEventToChatMessageConverter.formatEventWithDiff(
            recipient, commentCreatedEventWithMentionInCommentBody);

    // THEN
    assertNotNull(messageText);
    assertFalse(messageText.isEmpty());
    assertEquals(expectedResult, messageText);

    verify(commentCreatedEventWithMentionInCommentBody, times(1)).getEventTypeId();
    verify(jiraMarkdownToChatMarkdownConverter)
        .makeMyteamMarkdownFromJira(eq(createdCommentBody), eq(true));
    verify(messageFormatter).getIssueLink(eq("someKey"));
    verify(applicationProperties).getString(eq(APKeys.JIRA_BASEURL));
  }
}
