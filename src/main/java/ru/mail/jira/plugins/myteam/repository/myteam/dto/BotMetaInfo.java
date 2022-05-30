/* (C)2022 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BotMetaInfo {
  private String userId;
  private String nick;
  private String firstName;
  private String about;
}
