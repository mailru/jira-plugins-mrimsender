/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * example: { "slug":"repository", "id":84, "name":"repository", "scmId":"git", "state":"AVAILABLE",
 * "statusMessage":"Available", "forkable":true, "project":{ "key":"PROJ", "id":84,
 * "name":"project", "public":false, "type":"NORMAL" }, "public":false }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class RepositoryDto {
  private String slug;
  private long id;
  private String name;
  private String scmId;
  private String state;
  private String statusMessage;

  @JsonProperty("forkable")
  private boolean isForkable;

  private ProjectDto projectDto;

  @JsonProperty("public")
  private boolean isPublic;

  private RepositoryDto origin;
  private LinksDto links;
}
