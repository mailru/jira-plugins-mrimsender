/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.response;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusResponse {
  private boolean ok;
}
