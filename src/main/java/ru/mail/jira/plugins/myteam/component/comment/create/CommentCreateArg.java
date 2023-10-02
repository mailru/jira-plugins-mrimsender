/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.comment.create;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public final class CommentCreateArg {

  @NotNull private final Issue issueToComment;
  @NotNull private final ApplicationUser commentAuthor;
  @NotNull private final String body;
}
