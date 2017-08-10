package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Scanned
@Path("/calendar/events")
@Produces(MediaType.APPLICATION_JSON)
public class RestCalendarEventService {
    private final static Logger log = LoggerFactory.getLogger(RestCalendarEventService.class);

    private final CalendarEventService calendarEventService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final LicenseService licenseService;

    public RestCalendarEventService(
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        CalendarEventService calendarEventService,
        LicenseService licenseService
    ) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.calendarEventService = calendarEventService;
        this.licenseService = licenseService;
    }

    @GET
    @Path("{calendarId}/event/{eventId}/info")
    public Response getEventInfo(@PathParam("calendarId") final int calendarId,
                                 @PathParam("eventId") final String eventId) {
        return new RestExecutor<IssueInfo>() {
            @Override
            protected IssueInfo doAction() throws Exception {
                return calendarEventService.getEventInfo(jiraAuthenticationContext.getUser(), calendarId, eventId);
            }
        }.getResponse();
    }

    @GET
    @Path("{calendarId}")
    public Response getEvents(@PathParam("calendarId") final int calendarId,
                              @QueryParam("start") final String start,
                              @QueryParam("end") final String end) {
        try {
            if (log.isDebugEnabled())
                log.debug("getEvents with params. calendarId={}, start={}, end={}", new Object[]{calendarId, start, end});
            List<EventDto> result = calendarEventService.findEvents(calendarId, start, end, jiraAuthenticationContext.getUser());
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            cacheControl.setNoStore(true);
            cacheControl.setMaxAge(0);
            return Response.ok(result).cacheControl(cacheControl).build();
        } catch (Exception e) {
            log.error("Error while trying to get events", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{calendarId}/event/{eventId}/move")
    public Response moveEvent(@PathParam("calendarId") final int calendarId,
                              @PathParam("eventId") final String eventId,
                              @FormParam("start") final String start,
                              @FormParam("end") final String end) {
        return new RestExecutor<EventDto>() {
            @Override
            protected EventDto doAction() throws Exception {
                licenseService.checkLicense();
                ApplicationUser user = jiraAuthenticationContext.getUser();
                return calendarEventService.moveEvent(user, calendarId, eventId, StringUtils.trimToNull(start), StringUtils.trimToNull(end));
            }
        }.getResponse();
    }
}
