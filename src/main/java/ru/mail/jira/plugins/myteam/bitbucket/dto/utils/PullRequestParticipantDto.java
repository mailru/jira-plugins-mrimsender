/* (C)2021 */
package ru.mail.jira.plugins.myteam.bitbucket.dto.utils;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Example: { "user": { "name":"admin", "emailAddress":"admin@example.com", "id":1,
 * "displayName":"Administrator", "active":true, "slug":"admin", "type":"NORMAL" }, "role":"AUTHOR",
 * "approved":false, "status":"UNAPPROVED" }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PullRequestParticipantDto {
  private UserDto user;
  private String role;

  @JsonProperty("approved")
  private boolean isApproved;

  private String status;
}
