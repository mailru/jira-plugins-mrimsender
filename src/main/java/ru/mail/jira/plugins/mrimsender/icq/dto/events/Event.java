package ru.mail.jira.plugins.mrimsender.icq.dto.events;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CallbackQueryEvent.class, name = "callbackQuery"),
        @JsonSubTypes.Type(value = NewMessageEvent.class, name = "newMessage")
})
@Getter
@Setter
@ToString
public abstract class Event<T> {
    private long eventId;
    private T payload;
}
