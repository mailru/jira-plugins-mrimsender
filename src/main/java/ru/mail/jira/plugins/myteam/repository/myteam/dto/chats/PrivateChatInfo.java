/* (C)2021 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto.chats;

import lombok.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class PrivateChatInfo extends ChatInfoResponse {
  private String firstName;
  private String lastName;
  private String nick;
  private String about;
  private boolean isBot;
  private String language;
}
