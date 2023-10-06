/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.events.IcqEvent;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchResponse {
  @Nullable private List<IcqEvent> events;
  private boolean ok;

  @Nullable
  public List<IcqEvent> getEvents() {
    return events;
  }
}
