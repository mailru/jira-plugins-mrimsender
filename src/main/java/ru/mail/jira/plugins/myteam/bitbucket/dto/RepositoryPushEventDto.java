/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import java.util.List;
import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.ChangeDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RepositoryDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RepositoryPushEventDto extends BitbucketEventDto {
  private UserDto actor;
  private RepositoryDto repository;
  private List<ChangeDto> changes;
}
