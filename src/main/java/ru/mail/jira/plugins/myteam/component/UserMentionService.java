/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static java.util.stream.Collectors.toSet;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.mention.MentionFinder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Iterables;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class UserMentionService {
  private final MentionFinder mentionFinder;
  private final ChangeHistoryManager changeHistoryManager;

  private final UserManager userManager;

  public UserMentionService(
      @ComponentImport final MentionFinder mentionFinder,
      @ComponentImport final ChangeHistoryManager changeHistoryManager,
      UserManager userManager) {
    this.mentionFinder = mentionFinder;
    this.changeHistoryManager = changeHistoryManager;
    this.userManager = userManager;
  }

  public Set<ApplicationUser> getMentionedUsersInDescription(
      @NotNull final Issue issue, final boolean computePreviousValue) {
    final Set<ApplicationUser> mentionedUsers =
        getUsersMentionedInText(StringUtils.defaultString(issue.getDescription()));

    boolean containsDescriptionChanges = false;
    if (computePreviousValue) {
      final List<ChangeHistory> allChangeHistories = changeHistoryManager.getChangeHistories(issue);
      if (!allChangeHistories.isEmpty()) {
        final ChangeHistory last = Iterables.getLast(allChangeHistories);

        final List<ChangeItemBean> changeItemBeans = last.getChangeItemBeans();
        for (ChangeItemBean changeItemBean : changeItemBeans) {
          if (changeItemBean.getField().equals(IssueFieldConstants.DESCRIPTION)) {
            final Set<ApplicationUser> lastMentionedUsers =
                getUsersMentionedInText(StringUtils.defaultString(changeItemBean.getFromString()));
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
      return mentionedUsers;
    }
    return Collections.emptySet();
  }

  @NotNull
  public Set<ApplicationUser> getMentionedUserInComment(@Nullable Comment comment) {
    return getMentionedUserInEditedComment(comment, null);
  }

  @NotNull
  public Set<ApplicationUser> getMentionedUserInEditedComment(
      @Nullable final Comment comment, @Nullable final Comment originalComment) {
    if (comment == null) {
      return Collections.emptySet();
    }
    final Set<ApplicationUser> mentionedUsers =
        getUsersMentionedInText(StringUtils.defaultString(comment.getBody()));
    if (originalComment != null) {
      // if a comment was edited strip out any users that were already mentioned previously in this
      // comment
      final Set<ApplicationUser> originalMentionedUsers =
          getUsersMentionedInText(StringUtils.defaultString(originalComment.getBody()));
      mentionedUsers.removeAll(originalMentionedUsers);
    }
    return mentionedUsers;
  }

  private Set<ApplicationUser> getUsersMentionedInText(@NotNull final String mentionText) {
    return StreamSupport.stream(
            mentionFinder.getMentionedUsernames(mentionText).spliterator(), false)
        .filter(Objects::nonNull)
        .map(userManager::getUserByName)
        .filter(Objects::nonNull)
        .collect(toSet());
  }
}
