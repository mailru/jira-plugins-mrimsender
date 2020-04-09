package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        defaultImpl = JsonNode.class,
        include= JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = File.class, name = "file"),
        @JsonSubTypes.Type(value = Mention.class, name = "mention")
})
@Getter
@Setter
@ToString
public abstract class Part<T> {
    private T payload;
}
