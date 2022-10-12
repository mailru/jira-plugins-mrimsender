/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.events.IcqEvent;

@SuppressWarnings("NullAway")
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchResponse {
  private List<IcqEvent> events;
  private boolean ok;

  public List<IcqEvent> getEvents() {
    return events;
  }
}
