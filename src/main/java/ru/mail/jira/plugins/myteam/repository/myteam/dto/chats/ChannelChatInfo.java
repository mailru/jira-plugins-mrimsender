/* (C)2021 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto.chats;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class ChannelChatInfo extends ChatInfoResponse {
  private String title;
  private String about;
  private String rules;
  private String inviteLink;

  @JsonProperty("public")
  private boolean isPublic;

  private boolean joinModeration;
}
