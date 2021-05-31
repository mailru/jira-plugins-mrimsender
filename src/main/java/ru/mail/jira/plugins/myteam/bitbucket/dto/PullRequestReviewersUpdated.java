/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.PullRequestDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.PullRequestParticipantDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PullRequestReviewersUpdated extends BitbucketEventDto {
  private UserDto actor;
  private PullRequestDto pullRequest;
  private List<PullRequestParticipantDto> removedReviewers;
  private List<PullRequestParticipantDto> addedReviewers;
}
