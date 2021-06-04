/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codehaus.jackson.map.ObjectMapper;
import ru.mail.jira.plugins.myteam.bitbucket.dto.BitbucketEventDto;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class BitbucketEventDtoMessageBodyReader implements MessageBodyReader<BitbucketEventDto> {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean isReadable(
      Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return true;
  }

  @Override
  public BitbucketEventDto readFrom(
      Class<BitbucketEventDto> aClass,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> multivaluedMap,
      InputStream inputStream)
      throws IOException, WebApplicationException {
    return objectMapper.readValue(inputStream, BitbucketEventDto.class);
  }
}
