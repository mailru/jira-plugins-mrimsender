/* (C)2021 */
package ru.mail.jira.plugins.myteam.myteam.dto.chats;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings("NullAway")
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Member {
  public String userId;
  public boolean creator;
  public boolean admin;
}
