/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RefDto {
  private String id;
  private String displayId;
  private String type;
  private String latestCommit;
  private String latestChangeset;
  private RepositoryDto repository;

  @JsonProperty("public")
  private boolean isPublic;
}
