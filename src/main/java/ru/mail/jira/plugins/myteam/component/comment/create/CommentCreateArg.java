/* (C)2023 */
package ru.mail.jira.plugins.myteam.component.comment.create;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;

@Data
public final class CommentCreateArg {

  @NotNull private final Issue issueToComment;
  @NotNull private final ApplicationUser commentAuthor;
  @NotNull private final List<Part> messageParts;
  @Nullable private final String commentTemplate;
  @NotNull private final String formattedMainMessage;
}
