/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * example: "name":"admin", "emailAddress":"admin@example.com", "id":1,
 * "displayName":"Administrator", "active":true, "slug":"admin", "type":"NORMAL"
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class UserDto {
  private String name;
  private String emailAddress;
  private long id;
  private String displayName;

  @JsonProperty("active")
  private boolean isActive;

  private String slug;
  private String type;
}
