/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.Message;

@ToString
public class Forward extends Part<Forward.Data> {
  public Message getMessage() {
    return this.getPayload().message;
  }

  @Getter
  @Setter
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Data {
    public Message message;
  }
}
