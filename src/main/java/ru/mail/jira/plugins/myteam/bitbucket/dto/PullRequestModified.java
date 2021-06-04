/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.bitbucket.BitbucketWebhookEvent;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.PullRequestDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RefDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PullRequestModified extends BitbucketEventDto implements BitbucketWebhookEvent {
  private UserDto actor;
  private PullRequestDto pullRequest;
  private String previousTitle;
  private String previousDescription;
  private RefDto previousTarget;

  @Override
  public String getProjectName() {
    return pullRequest.getFromRef().getRepository().getProject().getName();
  }

  @Override
  public String getRepoSlug() {
    return pullRequest.getFromRef().getRepository().getSlug();
  }
}
