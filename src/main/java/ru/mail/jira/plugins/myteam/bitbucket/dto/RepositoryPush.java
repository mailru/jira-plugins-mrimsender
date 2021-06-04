/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.bitbucket.BitbucketWebhookEvent;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.ChangeDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RepositoryDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RepositoryPush extends BitbucketEventDto implements BitbucketWebhookEvent {
  private UserDto actor;
  private RepositoryDto repository;
  private List<ChangeDto> changes;

  @Override
  public String getProjectKey() {
    return repository.getProject().getKey();
  }

  @Override
  public String getRepoSlug() {
    return repository.getSlug();
  }
}
