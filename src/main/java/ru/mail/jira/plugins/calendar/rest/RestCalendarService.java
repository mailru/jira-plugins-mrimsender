package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.message.I18nResolver;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.extensions.property.RefreshInterval;
import net.fortuna.ical4j.extensions.property.WrCalName;
import net.fortuna.ical4j.model.*;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.Uris;
import net.fortuna.ical4j.validate.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.rest.dto.SingleValueDto;
import ru.mail.jira.plugins.calendar.common.Consts;
import ru.mail.jira.plugins.calendar.service.*;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarDto;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarSettingDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Scanned
@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class RestCalendarService {
    private final static Logger log = LoggerFactory.getLogger(RestCalendarService.class);

    private final CalendarService calendarService;
    private final CalendarEventService calendarEventService;

    private final I18nResolver i18nResolver;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final UserDataService userDataService;
    private final UserManager userManager;
    private final LicenseService licenseService;

    public RestCalendarService(
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        @ComponentImport UserManager userManager,
        CalendarService calendarService,
        CalendarEventService calendarEventService,
        JiraDeprecatedService jiraDeprecatedService,
        UserDataService userDataService,
        LicenseService licenseService
    ) {
        this.calendarService = calendarService;
        this.calendarEventService = calendarEventService;
        this.i18nResolver = i18nResolver;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.userDataService = userDataService;
        this.userManager = userManager;
        this.licenseService = licenseService;
    }

    @POST
    public Response createCalendar(final CalendarSettingDto calendarSettingDto) {
        return new RestExecutor<CalendarDto>() {
            @Override
            protected CalendarDto doAction() throws Exception {
                licenseService.checkLicense();
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
                licenseService.checkLicense();
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
                licenseService.checkLicense();
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
    @Path("/forUser/{calendarId}")
    public Response getUserCalendar(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<CalendarDto>() {
            @Override
            protected CalendarDto doAction() throws Exception {
                return calendarService.getUserCalendar(jiraAuthenticationContext.getLoggedInUser(), calendarId);
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
                licenseService.checkLicense();
                calendarService.updateCalendarVisibility(calendarId, jiraAuthenticationContext.getUser(), visible);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("/{calendarId}/addToFavouriteQuickFilter/{id}")
    public Response addToFavouriteQuickFilter(@PathParam("calendarId") final int calendarId,
                                              @PathParam("id") final int id,
                                              @FormParam("addToFavourite") final boolean addToFavourite) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.addToFavouriteQuickFilter(calendarId, jiraAuthenticationContext.getLoggedInUser(), id, addToFavourite);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("/{calendarId}/selectQuickFilter/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response selectQuickFilter(@PathParam("calendarId") final int calendarId,
                                      @PathParam("id") final int id,
                                      @FormParam("selected") final boolean selected) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.selectQuickFilter(calendarId, jiraAuthenticationContext.getLoggedInUser(), id, selected);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("/{calendarId}/selectQuickFilter/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectQuickFilterJson(
        @PathParam("calendarId") final int calendarId,
        @PathParam("id") final int id,
        final SingleValueDto<Boolean> data
    ) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.selectQuickFilter(calendarId, jiraAuthenticationContext.getLoggedInUser(), id, data.getValue());
                return null;
            }
        }.getResponse();
    }

    @GET
    @Produces("text/calendar")
    @Path("{icalUid}/{calendars}.ics")
    @AnonymousAllowed
    public Response getIcsCalendar(
        @PathParam("icalUid") final String icalUid,
        @PathParam("calendars") final String calendars,
        @QueryParam("issueKeys") boolean withIssueKeys
    ) {
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

                    //todo: check windows outlook & google calendar
                    DateTimeFormatter userDateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withZone(Consts.UTC_TZ).withStyle(DateTimeStyle.ISO_8601_DATE);
                    DateTimeFormatter userDateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
                    final net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
                    calendar.getProperties().add(new ProdId("-//MailRu Calendar/" + icalUid + "/iCal4j 1.0//EN"));
                    calendar.getProperties().add(Version.VERSION_2_0);
                    calendar.getProperties().add(CalScale.GREGORIAN);
                    calendar.getProperties().add(Method.PUBLISH);
                    ParameterList refreshParams = new ParameterList();
                    refreshParams.add(Value.DURATION);
                    calendar.getProperties().add(new RefreshInterval(refreshParams, "PT30M"));
                    calendar.getProperties().add(new XProperty("X-PUBLISHED-TTL", "PT30M"));
                    calendar.getProperties().add(new WrCalName(null, "Jira Calendar"));
                    calendar.getProperties().add(new Name(null, "Jira Calendar"));

                    LocalDate startSearch = LocalDate.now().minusMonths(3);
                    LocalDate endSearch = LocalDate.now().plusMonths(1);

                    for (String calendarId : calendarIds) {
                        List<EventDto> events = calendarEventService.findEvents(
                            Integer.parseInt(calendarId), null,
                            startSearch.toString("yyyy-MM-dd"),
                            endSearch.toString("yyyy-MM-dd"),
                            userManager.getUserByKey(userData.getUserKey()),
                            false
                        );

                        for (EventDto event : events) {
                            Date start;
                            try {
                                start = new DateTime(true);
                                start.setTime(userDateTimeFormat.parse(event.getStart()).getTime());
                            } catch (Exception e) {
                                start = new Date();
                                start.setTime(userDateFormat.parse(event.getStart()).getTime());
                            }
                            Date end = null;
                            if (event.getEnd() != null) {
                                try {
                                    end = new DateTime(true);
                                    end.setTime(userDateTimeFormat.parse(event.getEnd()).getTime());
                                } catch (Exception e) {
                                    end = new Date();
                                    end.setTime(userDateFormat.parse(event.getEnd()).getTime());
                                }
                            }

                            String title = event.getTitle();

                            if (withIssueKeys && event.getId() != null && event.getType() == EventDto.Type.ISSUE) {
                                title = event.getId() + " " + title;
                            }

                            VEvent vEvent = end != null ? new VEvent(start, end, title) : new VEvent(start, title);
                            vEvent.getProperties().add(new Uid(calendarId + "_" + event.getId()));
                            if (event.getType() == EventDto.Type.ISSUE) {
                                vEvent.getProperties().add(new Url(Uris.create(ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/browse/" + event.getId())));
                                if (event.getIssueInfo() != null)
                                    vEvent.getProperties().add(new Description(event.getIssueInfo().toFormatString(i18nResolver)));
                            }

                            if (event.getEnd() == null) {
                                vEvent.getProperties().add(new Duration(new Dur(1, 0, 0, 0)));
                            }

                            calendar.getComponents().add(vEvent);
                        }
                    }

                    return output -> {
                        try {
                            new CalendarOutputter(false).output(calendar, output);
                        } catch (ValidationException e) {
                            throw new IOException(e);
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
