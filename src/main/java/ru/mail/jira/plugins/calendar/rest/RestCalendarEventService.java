package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.PermissionService;
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

@Path("/calendar/events")
@Produces(MediaType.APPLICATION_JSON)
public class RestCalendarEventService {
    private final static Logger log = LoggerFactory.getLogger(RestCalendarEventService.class);

    private final CalendarEventService calendarEventService;
    private final CalendarService calendarService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final LicenseService licenseService;
    private final PermissionService permissionService;

    public RestCalendarEventService(
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            CalendarEventService calendarEventService,
            CalendarService calendarService, LicenseService licenseService,
            PermissionService permissionService) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.calendarEventService = calendarEventService;
        this.calendarService = calendarService;
        this.licenseService = licenseService;
        this.permissionService = permissionService;
    }

    private boolean canUseCalendar(ApplicationUser user, int calendarId) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);
        return permissionService.hasAdminPermission(user, calendar) || permissionService.hasUsePermission(user, calendar);
    }

    @GET
    @Path("/holidays")
    public Response getHolidays() {
        try {
            List<EventDto> result = calendarEventService.getHolidays(jiraAuthenticationContext.getUser());
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

    @GET
    @Path("{calendarId}/event/{eventId}/info")
    public Response getEventInfo(@PathParam("calendarId") final int calendarId,
                                 @PathParam("eventId") final String eventId) {
        return new RestExecutor<IssueInfo>() {
            @Override
            protected IssueInfo doAction() throws Exception {
                ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
                if (!canUseCalendar(currentUser, calendarId))
                    throw new SecurityException("No permission to view event info for this calendar");
                return calendarEventService.getEventInfo(currentUser, calendarId, eventId);
            }
        }.getResponse();
    }

    @GET
    @Path("{calendarId}")
    public Response getEvents(
        @PathParam("calendarId") final int calendarId,
        @QueryParam("start") final String start,
        @QueryParam("end") final String end,
        @QueryParam("groupBy") final String groupBy
    ) {
        try {
            if (log.isDebugEnabled())
                log.debug("getEvents with params. calendarId={}, start={}, end={}", calendarId, start, end);
            ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
            if (!canUseCalendar(currentUser, calendarId))
                throw new SecurityException("No permission to view events in this calendar");
            List<EventDto> result = calendarEventService.findEvents(calendarId, StringUtils.trimToNull(groupBy), start, end, currentUser);
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
                ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
                if (!canUseCalendar(currentUser, calendarId))
                    throw new SecurityException("No permission to move event in this calendar");
                return calendarEventService.moveEvent(currentUser, calendarId, eventId, StringUtils.trimToNull(start), StringUtils.trimToNull(end));
            }
        }.getResponse();
    }
}
