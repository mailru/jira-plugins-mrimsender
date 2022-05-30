/* (C)2020 */
package ru.mail.jira.plugins.myteam.repository.myteam.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageResponse {
  private long msgId;
  private boolean ok;
  private String description;
}
