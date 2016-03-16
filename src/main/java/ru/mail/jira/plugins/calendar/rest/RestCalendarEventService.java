package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.Event;
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.commons.RestExecutor;

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

    private final CalendarService calendarService;
    private final CalendarEventService calendarEventService;

    private final IssueService issueService;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public RestCalendarEventService(CalendarService calendarService,
                                    CalendarEventService calendarEventService,
                                    IssueService issueService,
                                    JiraAuthenticationContext jiraAuthenticationContext) {
        this.calendarService = calendarService;
        this.calendarEventService = calendarEventService;
        this.issueService = issueService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
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
                log.debug("getEvents with params. calendarId={}, start={}, end={}", calendarId, start, end);
            List<Event> result = calendarEventService.findEvents(calendarId, start, end, jiraAuthenticationContext.getUser());
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
    @Path("{calendarId}/event/{eventId}/")
    public Response moveEvent(@PathParam("calendarId") final int calendarId,
                              @PathParam("eventId") final String eventId,
                              @QueryParam("dayDelta") final int dayDelta,
                              @QueryParam("millisDelta") final int millisDelta,
                              @QueryParam("isDrag") final boolean isDrag) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getUser();
                IssueService.IssueResult issueResult = issueService.getIssue(ApplicationUsers.toDirectoryUser(user), eventId);
                MutableIssue issue = issueResult.getIssue();
                if (!issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)))
                    throw new IllegalArgumentException("Can not edit issue with key => " + eventId);

                Calendar calendar = calendarService.getCalendar(calendarId);
                if (isDrag)
                    calendarEventService.dragEvent(user, calendar, issue, dayDelta, millisDelta);
                else
                    calendarEventService.resizeEvent(user, calendar, issue, dayDelta, millisDelta);
                return null;
            }
        }.getResponse();
    }
}
