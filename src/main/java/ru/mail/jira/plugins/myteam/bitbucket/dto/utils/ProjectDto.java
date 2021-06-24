/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/** "key":"PROJ", "id":84, "name":"project", "public":false, "type":"NORMAL" */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ProjectDto {
  private String key;
  private long id;
  private String name;

  @JsonProperty("public")
  private boolean isPublic;

  private String type;
  private UserDto owner;
}
