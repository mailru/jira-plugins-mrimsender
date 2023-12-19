/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.mention.MentionFinder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PluginMentionService {
  private final MentionFinder mentionFinder;
  private final ChangeHistoryManager changeHistoryManager;

  public PluginMentionService(
      @ComponentImport final MentionFinder mentionFinder,
      @ComponentImport final ChangeHistoryManager changeHistoryManager) {
    this.mentionFinder = mentionFinder;
    this.changeHistoryManager = changeHistoryManager;
  }

  public boolean checkMentionUserInDescription(
      @NotNull final Issue issue,
      @NotNull final ApplicationUser mentionedUser,
      final boolean computePreviousValue) {
    final String userNameToSearchInMentionText = mentionedUser.getUsername();
    final Set<String> mentionedUsers =
        getUsersMentionedInText(
            StringUtils.defaultString(issue.getDescription()), userNameToSearchInMentionText);

    boolean containsDescriptionChanges = false;
    if (computePreviousValue) {
      final List<ChangeHistory> allChangeHistories = changeHistoryManager.getChangeHistories(issue);
      if (!allChangeHistories.isEmpty()) {
        final ChangeHistory last = Iterables.getLast(allChangeHistories);

        final List<ChangeItemBean> changeItemBeans = last.getChangeItemBeans();
        for (ChangeItemBean changeItemBean : changeItemBeans) {
          if (changeItemBean.getField().equals(IssueFieldConstants.DESCRIPTION)) {
            final Set<String> lastMentionedUsers =
                getUsersMentionedInText(
                    StringUtils.defaultString(changeItemBean.getFromString()),
                    userNameToSearchInMentionText);
            mentionedUsers.removeAll(lastMentionedUsers);
            containsDescriptionChanges = true;
            break;
          }
        }
      }
    } else {
      containsDescriptionChanges = true;
    }

    if (!mentionedUsers.isEmpty() && containsDescriptionChanges) {
      return true;
    }
    return false;
  }

  public boolean checkMentionUserInComment(
      @NotNull final ApplicationUser mentionedUser,
      @Nullable final Comment comment,
      @Nullable final Comment originalComment) {
    return getCommentMentions(mentionedUser.getUsername(), comment, originalComment).size() != 0;
  }

  @NotNull
  private Set<String> getCommentMentions(
      @NotNull final String findUserName,
      @Nullable final Comment comment,
      @Nullable final Comment originalComment) {
    // in the case of an edit, lets pass along the original comment since we can't get it any other
    // way. Change history doesn't do comments.
    if (comment == null) {
      return emptySet();
    }
    final Set<String> mentionedUsers =
        getUsersMentionedInText(StringUtils.defaultString(comment.getBody()), findUserName);
    if (originalComment != null) {
      // if a comment was edited strip out any users that were already mentioned previously in this
      // comment
      final Set<String> originalMentionedUsers =
          getUsersMentionedInText(
              StringUtils.defaultString(originalComment.getBody()), findUserName);
      mentionedUsers.removeAll(originalMentionedUsers);
    }
    return mentionedUsers;
  }

  private Set<String> getUsersMentionedInText(
      @NotNull final String mentionText, final String findUserName) {
    return StreamSupport.stream(
            mentionFinder.getMentionedUsernames(mentionText).spliterator(), false)
        .filter(Objects::nonNull)
        .filter(mentionUserName -> mentionUserName.equals(findUserName))
        .collect(toSet());
  }
}
