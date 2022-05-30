/* (C)2020 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chat {
  private String chatId;
  private String title;
  private String type;
}
