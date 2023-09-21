/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.comment.create;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class CommentCreateArg {

  @NotNull private final Issue issueToComment;
  @NotNull private final ApplicationUser commentAuthor;
  @NotNull private final String body;
}
