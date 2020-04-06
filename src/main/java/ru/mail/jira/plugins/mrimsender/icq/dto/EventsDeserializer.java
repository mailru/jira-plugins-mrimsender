package ru.mail.jira.plugins.mrimsender.icq.dto;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import ru.mail.jira.plugins.mrimsender.icq.Consts;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.DeletedMessageEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.EditedMessageEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.LeftChatMembersEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewChatMembersEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.PinnedMessageEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.UnpinnedMessageEvent;

import javax.annotation.Nonnull;
import java.io.IOException;

public class EventsDeserializer extends StdDeserializer<Event<?>> {
    public EventsDeserializer() {
        this(Event.class);
    }

    public EventsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Nonnull
    private String extractEventType(final JsonNode jsonEvent) {
        if (!jsonEvent.isObject()) {
            return "";
        }
        ObjectNode eventObj = (ObjectNode) jsonEvent;
        JsonNode typeNode = eventObj.get("type");

        if (typeNode == null || !typeNode.isTextual())
            return "";

        TextNode textNode = (TextNode) typeNode;
        return textNode.getTextValue();
    }

    @Override
    public Event<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String eventType = extractEventType(node);
        if (eventType.equals(Consts.EventType.NEW_MESSAGE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, NewMessageEvent.class);
        }
        if (eventType.equals(Consts.EventType.DELETED_MESSAGE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, DeletedMessageEvent.class);
        }
        if (eventType.equals(Consts.EventType.EDITED_MESSAGE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, EditedMessageEvent.class);
        }
        if (eventType.equals(Consts.EventType.LEFT_CHAT_MEMBERS_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, LeftChatMembersEvent.class);
        }
        if (eventType.equals(Consts.EventType.NEW_CHAT_MEMBERS_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, NewChatMembersEvent.class);
        }
        if (eventType.equals(Consts.EventType.PINNED_MESSAGE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, PinnedMessageEvent.class);
        }
        if (eventType.equals(Consts.EventType.UNPINNED_MESSAGE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, UnpinnedMessageEvent.class);
        }
        if (eventType.equals(Consts.EventType.CALLBACK_QUERY_TYPE.getTypeStrValue())) {
            return ((ObjectMapper)jsonParser.getCodec()).readValue(node, CallbackQueryEvent.class);
        }
        return null;
    }
}