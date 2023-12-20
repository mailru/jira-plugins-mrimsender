/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.mention.MentionFinderImpl;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("MockNotUsedInProduction")
class PluginMentionServiceTest {

  @SuppressWarnings("NullAway")
  private PluginMentionService pluginMentionService;

  @SuppressWarnings("NullAway")
  private ChangeHistoryManager changeHistoryManager;

  @SuppressWarnings("NullAway")
  private UserManager userManager;

  @BeforeEach
  void setUp() {
    ChangeHistoryManager changeHistoryManager = mock(ChangeHistoryManager.class);
    this.changeHistoryManager = changeHistoryManager;
    UserManager userManager = mock(UserManager.class);
    this.userManager = userManager;
    pluginMentionService =
        new PluginMentionService(new MentionFinderImpl(), changeHistoryManager, userManager);
  }

  @Test
  void checkMentionUserInDescriptionWhenDescriptionIsNull() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getDescription()).thenReturn(null);
    when(changeHistoryManager.getChangeHistories(eq(issue))).thenReturn(List.of());

    // WHEN
    Set<ApplicationUser> result = pluginMentionService.getMentionedUsersInDescription(issue, true);

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  void checkMentionUserInDescriptionWhenNewDescriptionHasMentionIfOldDescHasNotMention() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getDescription()).thenReturn("[~admin]");
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.getUsername()).thenReturn("admin");
    ChangeHistory changeHistory = mock(ChangeHistory.class);
    ChangeItemBean changeItemBean = mock(ChangeItemBean.class);
    when(changeItemBean.getField()).thenReturn("description");
    when(changeItemBean.getFromString()).thenReturn("oldDescValue");

    when(changeHistory.getChangeItemBeans()).thenReturn(List.of(changeItemBean));
    when(changeHistoryManager.getChangeHistories(eq(issue))).thenReturn(List.of(changeHistory));
    ApplicationUser mentionedUser = mock(ApplicationUser.class);
    when(userManager.getUserByName(eq("admin"))).thenReturn(mentionedUser);

    // WHEN
    Set<ApplicationUser> result = pluginMentionService.getMentionedUsersInDescription(issue, true);

    // THEN
    assertFalse(result.isEmpty());
    assertEquals(Set.of(mentionedUser), result);
  }

  @Test
  void
      checkMentionUserInDescriptionWhenNewDescriptionHasMentionAndChangesHistoryNotHasDescriptionAsChangesOnIssue() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getDescription()).thenReturn("[~admin]");
    ApplicationUser recipient = mock(ApplicationUser.class);
    when(recipient.getUsername()).thenReturn("admin");
    ChangeHistory changeHistory = mock(ChangeHistory.class);
    ChangeItemBean changeItemBean = mock(ChangeItemBean.class);
    when(changeItemBean.getField()).thenReturn("customfield_10000");

    when(changeHistory.getChangeItemBeans()).thenReturn(List.of(changeItemBean));
    when(changeHistoryManager.getChangeHistories(eq(issue))).thenReturn(List.of(changeHistory));
    when(userManager.getUserByName(eq("admin"))).thenReturn(mock(ApplicationUser.class));

    // WHEN
    Set<ApplicationUser> result = pluginMentionService.getMentionedUsersInDescription(issue, true);

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  void checkMentionUserInDescriptionWhenNewDescriptionHasMentionAndComputePreviousValueIsFalse() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getDescription()).thenReturn("[~admin]");

    boolean computePreviousValue = false;

    ApplicationUser mentionedUser = mock(ApplicationUser.class);
    when(userManager.getUserByName(eq("admin"))).thenReturn(mentionedUser);

    // WHEN
    Set<ApplicationUser> result =
        pluginMentionService.getMentionedUsersInDescription(issue, computePreviousValue);

    // THEN
    assertFalse(result.isEmpty());
    assertEquals(Set.of(mentionedUser), result);
  }

  @Test
  void checkMentionUserInDescriptionWhenNewDescriptionHasMentionIfOldDescHasMention() {
    // GIVEN
    Issue issue = mock(Issue.class);
    when(issue.getDescription()).thenReturn("[~admin]");
    ChangeHistory changeHistory = mock(ChangeHistory.class);
    ChangeItemBean changeItemBean = mock(ChangeItemBean.class);
    when(changeItemBean.getField()).thenReturn("description");
    when(changeItemBean.getFromString()).thenReturn("[~admin]");

    when(changeHistory.getChangeItemBeans()).thenReturn(List.of(changeItemBean));
    when(changeHistoryManager.getChangeHistories(eq(issue))).thenReturn(List.of(changeHistory));

    when(userManager.getUserByName(eq("admin"))).thenReturn(mock(ApplicationUser.class));

    // WHEN
    Set<ApplicationUser> result = pluginMentionService.getMentionedUsersInDescription(issue, true);

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  void checkMentionUserInCommentWhenNewCommentAndOriginalCommentAreNull() {
    // GIVEN
    Comment comment = null;
    Comment originalComment = null;

    // WHEN
    Set<ApplicationUser> result =
        pluginMentionService.getMentionedUserInEditedComment(comment, originalComment);

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  void checkMentionUserInCommentWhenNewCommentHasMentionAndOriginalCommentIsNull() {
    // GIVEN
    Comment newComment = mock(Comment.class);
    when(newComment.getBody()).thenReturn("[~admin]");
    ApplicationUser mentionedUser = mock(ApplicationUser.class);
    when(userManager.getUserByName(eq("admin"))).thenReturn(mentionedUser);

    // WHEN
    Set<ApplicationUser> result = pluginMentionService.getMentionedUserInComment(newComment);

    // THEN
    assertFalse(result.isEmpty());
    assertEquals(Set.of(mentionedUser), result);
  }

  @Test
  void checkMentionUserInCommentWhenNewCommentHasMentionAndOldCommentHasNotMention() {
    // GIVEN
    Comment newComment = mock(Comment.class);
    when(newComment.getBody()).thenReturn("text\n\n\n\n\ntext123123__0[~admin]text");
    Comment originalComment = mock(Comment.class);
    when(originalComment.getBody()).thenReturn("some text");
    ApplicationUser mentionedUser = mock(ApplicationUser.class);
    when(userManager.getUserByName(eq("admin"))).thenReturn(mentionedUser);

    // WHEN
    Set<ApplicationUser> result =
        pluginMentionService.getMentionedUserInEditedComment(newComment, originalComment);

    // THEN
    assertFalse(result.isEmpty());
    assertEquals(Set.of(mentionedUser), result);
  }

  @Test
  void checkMentionUserInCommentWhenNewCommentHasMentionAndOldCommentHasMention() {
    // GIVEN
    Comment newComment = mock(Comment.class);
    when(newComment.getBody()).thenReturn("text\n\n\n\n\ntext123123__0[~admin]text");
    Comment originalComment = mock(Comment.class);
    when(originalComment.getBody())
        .thenReturn("text\n\n\n\n\ntext123123__0[~admin]\n\n\n\n45645654text");

    ApplicationUser mentionedUser = mock(ApplicationUser.class);
    when(userManager.getUserByName(eq("admin"))).thenReturn(mentionedUser);
    // WHEN
    Set<ApplicationUser> result =
        pluginMentionService.getMentionedUserInEditedComment(newComment, originalComment);

    // THEN
    assertTrue(result.isEmpty());
  }
}
