/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.RepositoryDto;
import ru.mail.jira.plugins.myteam.bitbucket.dto.utils.UserDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RepositoryModifiedEventDto extends BitbucketEventDto {
  @JsonProperty("old")
  private RepositoryDto oldRepo;

  @JsonProperty("new")
  private RepositoryDto newRepo;

  private UserDto actor;
}
