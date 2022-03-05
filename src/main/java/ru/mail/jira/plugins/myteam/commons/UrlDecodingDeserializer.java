/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UrlDecodingDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectCodec objectCodec = p.getCodec();
    JsonNode node = objectCodec.readTree(p);
    return URLDecoder.decode(node.asText(), StandardCharsets.UTF_8.name());
  }
}
