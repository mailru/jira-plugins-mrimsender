/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@SuppressWarnings("NullAway")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    defaultImpl = Part.class,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = File.class, name = "file"),
  @JsonSubTypes.Type(value = Mention.class, name = "mention"),
  @JsonSubTypes.Type(value = Forward.class, name = "forward"),
  @JsonSubTypes.Type(value = Reply.class, name = "reply")
})
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Part<T> {
  private T payload;
}
