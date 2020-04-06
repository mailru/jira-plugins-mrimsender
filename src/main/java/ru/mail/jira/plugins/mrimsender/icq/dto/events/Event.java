package ru.mail.jira.plugins.mrimsender.icq.dto.events;


import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import ru.mail.jira.plugins.mrimsender.icq.dto.EventsDeserializer;

@JsonDeserialize(using = EventsDeserializer.class)
/*@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CallbackQueryEvent.class, name = "callbackQuery"),
        @JsonSubTypes.Type(value = NewMessageEvent.class, name = "newMessage")
})*/
public abstract class Event<T> {
    private long eventId;
    private String type;
    private T payload;

    public long getEventId() {
        return eventId;
    }

    public String getType() {
        return type;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
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
        return "Event{" +
                "eventId=" + eventId +
                ", type='" + type + '\'' +
                ", payload=" + payload +
                '}';
    }
}
