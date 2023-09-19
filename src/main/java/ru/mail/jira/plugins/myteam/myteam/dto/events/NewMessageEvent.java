/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.events;

import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.Chat;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;

@SuppressWarnings("NullAway")
public class NewMessageEvent extends IcqEvent<NewMessageEvent.Data> {
  public long getMsgId() {
    return this.getPayload().msgId;
  }

  public long getTimestamp() {
    return this.getPayload().timestamp;
  }

  public String getText() {
    return this.getPayload().text;
  }

  public Chat getChat() {
    return this.getPayload().chat;
  }

  public User getFrom() {
    return this.getPayload().from;
  }

  public List<Part> getParts() {
    return this.getPayload().parts;
  }


  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Data {
    public long msgId;
    public long timestamp;
    public String text;
    public Chat chat;
    public User from;
    public List<Part> parts;

    @Override
    public String toString() {
      return "Data{"
          + "msgId="
          + msgId
          + ", timestamp="
          + timestamp
          + ", text='"
          + text
          + '\''
          + ", chat="
          + chat
          + ", from="
          + from
          + ", parts="
          + parts
          + '}';
    }
  }
}
