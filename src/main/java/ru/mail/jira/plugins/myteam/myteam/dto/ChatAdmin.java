/* (C)2022 */
package ru.mail.jira.plugins.myteam.myteam.dto;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@SuppressWarnings("NullAway")
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatAdmin {
  private String userId;
  private boolean creator;
}
