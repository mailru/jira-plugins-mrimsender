package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.Uris;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarDto;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarSettingDto;
import ru.mail.jira.plugins.calendar.rest.dto.Event;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.JiraDeprecatedService;
import ru.mail.jira.plugins.calendar.service.UserDataService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class RestCalendarService {
    private final static Logger log = LoggerFactory.getLogger(RestCalendarService.class);

    private final CalendarService calendarService;
    private final CalendarEventService calendarEventService;

    private final I18nHelper i18nHelper;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final UserDataService userDataService;
    private final UserManager userManager;


    public RestCalendarService(CalendarService calendarService,
                               CalendarEventService calendarEventService,
                               I18nHelper i18nHelper,
                               JiraAuthenticationContext jiraAuthenticationContext,
                               JiraDeprecatedService jiraDeprecatedService, UserDataService userDataService, UserManager userManager) {
        this.calendarService = calendarService;
        this.calendarEventService = calendarEventService;
        this.i18nHelper = i18nHelper;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.userDataService = userDataService;
        this.userManager = userManager;
    }

    @POST
    public Response createCalendar(final CalendarSettingDto calendarSettingDto) {
        return new RestExecutor<CalendarDto>() {
            @Override
            protected CalendarDto doAction() throws Exception {
                return calendarService.createCalendar(jiraAuthenticationContext.getUser(), calendarSettingDto);
            }
        }.getResponse();
    }

    @GET
    @Path("{calendarId}")
    public Response getCalendar(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<CalendarSettingDto>() {
            @Override
            protected CalendarSettingDto doAction() throws Exception {
                return calendarService.getCalendarSettingDto(jiraAuthenticationContext.getUser(), calendarId);
            }
        }.getResponse();
    }

    @PUT
    @Path("{id}")
    public Response updateCalendar(final CalendarSettingDto calendarSettingDto) {
        return new RestExecutor<CalendarDto>() {
            @Override
            protected CalendarDto doAction() throws Exception {
                return calendarService.updateCalendar(jiraAuthenticationContext.getUser(), calendarSettingDto);
            }
        }.getResponse();
    }

    @DELETE
    @Path("{id}")
    public Response deleteCalendar(@PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.deleteCalendar(jiraAuthenticationContext.getUser(), id);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("all")
    public Response getAllCalendars() {
        return new RestExecutor<CalendarDto[]>() {
            @Override
            protected CalendarDto[] doAction() throws Exception {
                return calendarService.getAllCalendars(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @GET
    @Path("find")
    public Response findCalendars(@QueryParam("id") final Set<Integer> calendarIds) {
        return new RestExecutor<CalendarDto[]>() {
            @Override
            protected CalendarDto[] doAction() throws Exception {
                return calendarService.findCalendars(jiraAuthenticationContext.getUser(), calendarIds.toArray(new Integer[calendarIds.size()]));
            }
        }.getResponse();
    }

    @GET
    @Path("forUser")
    public Response getUserCalendars() {
        return new RestExecutor<CalendarDto[]>() {
            @Override
            protected CalendarDto[] doAction() throws Exception {
                return calendarService.getUserCalendars(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @PUT
    @Path("/{calendarId}/visibility/{visible}")
    public Response invertVisibility(@PathParam("calendarId") final int calendarId,
                                     @PathParam("visible") final boolean visible) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.updateCalendarVisibility(calendarId, jiraAuthenticationContext.getUser(), visible);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Produces("text/calendar")
    @Path("{icalUid}/{calendars}.ics")
    @AnonymousAllowed
    public Response getIcsCalendar(@PathParam("icalUid") final String icalUid,
                                   @PathParam("calendars") final String calendars) {
        return new RestExecutor<StreamingOutput>() {
            @Override
            protected StreamingOutput doAction() throws Exception {
                try {
                    String[] calendarIds = StringUtils.split(calendars, "-");

                    UserData userData = userDataService.getUserDataByIcalUid(icalUid);
                    if (userData == null)
                        return null;
                    ApplicationUser user = userManager.getUserByKey(userData.getUserKey());
                    if (user == null || !user.isActive())
                        return null;

                    DateTimeFormatter userDateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE);
                    DateTimeFormatter userDateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
                    final net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
                    calendar.getProperties().add(new ProdId("-//MailRu Calendar/" + icalUid + "/iCal4j 1.0//EN"));
                    calendar.getProperties().add(Version.VERSION_2_0);
                    calendar.getProperties().add(CalScale.GREGORIAN);

                    LocalDate startSearch = LocalDate.now().minusMonths(3);
                    LocalDate endSearch = LocalDate.now().plusMonths(1);

                    for (String calendarId : calendarIds) {
                        List<Event> events = calendarEventService.findEvents(Integer.parseInt(calendarId),
                                                                             startSearch.toString("yyyy-MM-dd"),
                                                                             endSearch.toString("yyyy-MM-dd"),
                                                                             userManager.getUserByKey(userData.getUserKey()),
                                                                             true);

                        for (Event event : events) {
                            DateTime start;
                            try {
                                start = new DateTime(userDateTimeFormat.parse(event.getStart()));
                            } catch (Exception e) {
                                start = new DateTime(userDateFormat.parse(event.getStart()));
                            }
                            DateTime end = null;
                            if (event.getEnd() != null)
                                try {
                                    end = new DateTime(userDateTimeFormat.parse(event.getEnd()));
                                } catch (Exception e) {
                                    end = new DateTime(userDateFormat.parse(event.getEnd()));
                                }

                            VEvent vEvent = end != null ? new VEvent(start, end, event.getTitle()) : new VEvent(start, event.getTitle());
                            vEvent.getProperties().add(new Uid(calendarId + "_" + event.getId()));
                            vEvent.getProperties().add(new Url(Uris.create(ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/browse/" + event.getId())));
                            if (event.getIssueInfo() != null)
                                vEvent.getProperties().add(new Description(event.getIssueInfo().toFormatString(i18nHelper)));
                            calendar.getComponents().add(vEvent);
                        }
                    }

                    return new StreamingOutput() {
                        @Override
                        public void write(OutputStream output) throws IOException, WebApplicationException {
                            try {
                                new CalendarOutputter().output(calendar, output);
                            } catch (ValidationException e) {
                                throw new IOException(e);
                            }
                        }
                    };
                } catch (Throwable t) {
                    log.error("Export ics calendar", t);
                    return null;
                }
            }
        }.getResponse();
    }
}
