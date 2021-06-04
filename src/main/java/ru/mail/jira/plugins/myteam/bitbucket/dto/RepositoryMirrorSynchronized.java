/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.mail.jira.plugins.myteam.bitbucket.BitbucketWebhookEvent;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.ChangeDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RepositoryDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RepositoryMirrorSynchronized extends BitbucketEventDto implements BitbucketWebhookEvent {
  private Void mirrorServer;
  private String syncType;
  private RepositoryDto repository;
  private List<ChangeDto> changes;

  @JsonProperty("refLimitExceeded")
  private boolean isRefLimitExceeded;

  @Override
  public String getProjectName() {
    return repository.getProject().getName();
  }

  @Override
  public String getRepoSlug() {
    return repository.getSlug();
  }
}
