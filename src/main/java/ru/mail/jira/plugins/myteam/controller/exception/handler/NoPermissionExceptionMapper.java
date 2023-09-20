/* (C)2023 */
package ru.mail.jira.plugins.myteam.controller.exception.handler;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import javax.naming.NoPermissionException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import ru.mail.jira.plugins.commons.dto.ErrorDto;

@Provider
public class NoPermissionExceptionMapper implements ExceptionMapper<NoPermissionException> {
  @Override
  public Response toResponse(NoPermissionException exception) {
    return Response.status(Response.Status.FORBIDDEN)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(
            ErrorDto.builder()
                .error(Objects.requireNonNullElse(exception.getMessage(), ""))
                .status(Response.Status.FORBIDDEN.getStatusCode())
                .timestamp(Date.from(Instant.now()))
                .build())
        .build();
  }
}
