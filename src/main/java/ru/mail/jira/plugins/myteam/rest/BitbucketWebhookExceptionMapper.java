/* (C)2021 */
package ru.mail.jira.plugins.myteam.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import ru.mail.jira.plugins.myteam.rest.dto.BitbucketWebhookResultDto;

@Provider
public class BitbucketWebhookExceptionMapper implements ExceptionMapper<BitbucketWebhookException> {
  @Override
  public Response toResponse(BitbucketWebhookException e) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(new BitbucketWebhookResultDto(e.getMessage()))
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }
}
