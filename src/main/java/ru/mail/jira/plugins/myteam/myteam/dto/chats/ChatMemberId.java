/* (C)2021 */
package ru.mail.jira.plugins.myteam.myteam.dto.chats;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@SuppressWarnings("NullAway")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMemberId {
  private String sn;

  public ChatMemberId(String email) {
    this.sn = email;
  }
}
