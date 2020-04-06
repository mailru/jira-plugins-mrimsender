package ru.mail.jira.plugins.mrimsender.icq.dto.parts;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.PartsDeserializer;


@JsonDeserialize(using = PartsDeserializer.class)
/*@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = File.class, name = "file"),
        @JsonSubTypes.Type(value = Mention.class, name = "mention")
})*/
public abstract class Part<T> {
    private String type;
    private T payload;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "Part{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                '}';
    }
}
