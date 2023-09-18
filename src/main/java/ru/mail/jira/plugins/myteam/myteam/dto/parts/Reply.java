/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.parts;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.TextFormatMetadata;
import ru.mail.jira.plugins.myteam.myteam.dto.User;

@SuppressWarnings("NullAway")
@ToString
public class Reply extends Part<Reply.Data> {
  public ReplyMessage getMessage() {
    return this.getPayload().message;
  }

  @Getter
  @Setter
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Data {
    public ReplyMessage message;
  }

  @Getter
  @Setter
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ReplyMessage {
    private User from;
    private long msgId;
    private String text;
    private long timestamp;
    private List<Part> parts;
    private TextFormatMetadata format;
  }
}
