/* (C)2021 */
package ru.mail.jira.plugins.myteam.myteam.dto.chats;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatMember {
  public List<Member> members;
  public boolean ok;
}
