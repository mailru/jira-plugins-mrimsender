package ru.mail.jira.plugins.mrimsender.icq.dto;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import ru.mail.jira.plugins.mrimsender.icq.Consts;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.File;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Forward;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Mention;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Part;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Reply;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Sticker;
import ru.mail.jira.plugins.mrimsender.icq.dto.parts.Voice;

import javax.annotation.Nonnull;
import java.io.IOException;

public class PartsDeserializer extends StdDeserializer<Part<?>> {
    public PartsDeserializer() {
        this(Part.class);
    }

    public PartsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Nonnull
    private static String extractMessagePartType(final JsonNode jsonMessagePart) {
        if (!jsonMessagePart.isObject())
            return "";
        ObjectNode messagePartObj = (ObjectNode) jsonMessagePart;
        JsonNode typeNode = messagePartObj.get("type");

        if (typeNode == null || !typeNode.isTextual())
            return "";

        TextNode textNode = (TextNode) typeNode;
        return textNode.getTextValue();
    }

    @Override
    public Part<?> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.readValueAsTree();
        String partType = extractMessagePartType(node);
        if (partType.equals(Consts.PartType.FILE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, File.class);
        }
        if (partType.equals(Consts.PartType.FORWARD_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, Forward.class);
        }
        if (partType.equals(Consts.PartType.MENTION_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, Mention.class);
        }
        if (partType.equals(Consts.PartType.REPLY_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, Reply.class);
        }
        if (partType.equals(Consts.PartType.STICKER_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, Sticker.class);
        }
        if (partType.equals(Consts.PartType.VOICE_TYPE.getTypeStrValue())) {
            return ((ObjectMapper) jsonParser.getCodec()).readValue(node, Voice.class);
        }
        return null;
    }
}