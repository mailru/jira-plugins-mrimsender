/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

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
public class PullRequestUnapprovedByReviewer extends BitbucketEventDto {
  private UserDto actor;
  private PullRequestDto pullRequest;
  private PullRequestParticipantDto participant;
  private String previousStatus;
}
