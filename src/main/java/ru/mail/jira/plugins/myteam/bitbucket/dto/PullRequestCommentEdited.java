/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.CommentDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.PullRequestDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PullRequestCommentEdited extends BitbucketEventDto {
  private UserDto actor;
  private PullRequestDto pullRequest;
  private CommentDto commentDto;
  private long commentParentId;

  @JsonProperty("previousComment")
  private String previousCommentText;
}
