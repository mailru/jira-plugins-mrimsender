package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.QuickFilter;
import ru.mail.jira.plugins.calendar.rest.dto.QuickFilterDto;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.PermissionService;
import ru.mail.jira.plugins.calendar.service.QuickFilterService;
import ru.mail.jira.plugins.calendar.service.UserCalendarService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/calendar/{calendarId}/quickFilter")
@Produces(MediaType.APPLICATION_JSON)
public class RestQuickFilterService {
    private final CalendarService calendarService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserCalendarService userCalendarService;
    private final PermissionService permissionService;
    private final QuickFilterService quickFilterService;

    public RestQuickFilterService(
            CalendarService calendarService,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            UserCalendarService userCalendarService,
            PermissionService permissionService,
            QuickFilterService quickFilterService
    ) {
        this.calendarService = calendarService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userCalendarService = userCalendarService;
        this.permissionService = permissionService;
        this.quickFilterService = quickFilterService;
    }

    private boolean canUseCalendar(ApplicationUser user, int calendarId) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);
        return permissionService.hasAdminPermission(user, calendar) || permissionService.hasUsePermission(user, calendar);
    }

    @POST
    public Response createQuickFilter(@PathParam("calendarId") final int calendarId, final QuickFilterDto quickFilterDto) {
        return new RestExecutor<QuickFilterDto>() {
            @Override
            protected QuickFilterDto doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
                if (!canUseCalendar(user, calendarId))
                    throw new SecurityException("No permission to create quick filter for this calendar");
                List<QuickFilter> favouriteQuickFilters = userCalendarService.getFavouriteQuickFilters(calendarId, user.getKey());
                QuickFilter quickFilter = quickFilterService.createQuickFilter(calendarId, quickFilterDto.getName(), quickFilterDto.getJql(), quickFilterDto.getDescription(), quickFilterDto.isShare(), user);
                return new QuickFilterDto(quickFilter, quickFilter.getCreatorKey().equals(user.getKey()), favouriteQuickFilters != null && favouriteQuickFilters.contains(quickFilter));
            }
        }.getResponse();
    }

    @GET
    @Path("{id}")
    public Response getQuickFilter(@PathParam("calendarId") final int calendarId, @PathParam("id") final int id) {
        return new RestExecutor<QuickFilterDto>() {
            @Override
            protected QuickFilterDto doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
                List<QuickFilter> favouriteQuickFilters = userCalendarService.getFavouriteQuickFilters(calendarId, user.getKey());
                QuickFilter quickFilter = quickFilterService.getQuickFilter(id);
                return new QuickFilterDto(quickFilter, quickFilter.getCreatorKey().equals(user.getKey()), favouriteQuickFilters != null && favouriteQuickFilters.contains(quickFilter));
            }
        }.getResponse();
    }

    @PUT
    @Path("{id}")
    public Response updateQuickFilter(@PathParam("calendarId") final int calendarId, @PathParam("id") final int id, final QuickFilterDto quickFilterDto) {
        return new RestExecutor<QuickFilterDto>() {
            @Override
            protected QuickFilterDto doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
                List<QuickFilter> favouriteQuickFilters = userCalendarService.getFavouriteQuickFilters(calendarId, user.getKey());
                QuickFilter quickFilter = quickFilterService.updateQuickFilter(id, calendarId, quickFilterDto.getName(), quickFilterDto.getJql(), quickFilterDto.getDescription(), quickFilterDto.isShare(), user);
                return new QuickFilterDto(quickFilter, quickFilter.getCreatorKey().equals(user.getKey()), favouriteQuickFilters != null && favouriteQuickFilters.contains(quickFilter));
            }
        }.getResponse();
    }

    @DELETE
    @Path("{id}")
    public Response deleteQuickFilter(@PathParam("calendarId") final int calendarId, @PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                quickFilterService.deleteQuickFilterById(id, jiraAuthenticationContext.getLoggedInUser());
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("all")
    public Response getAllQuickFilters(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<List<QuickFilterDto>>() {
            @Override
            protected List<QuickFilterDto> doAction() throws Exception {
                List<QuickFilterDto> result = new ArrayList<QuickFilterDto>();
                ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
                List<QuickFilter> favouriteQuickFilters = userCalendarService.getFavouriteQuickFilters(calendarId, user.getKey());
                for (QuickFilter quickFilter : quickFilterService.getQuickFilters(calendarId, jiraAuthenticationContext.getLoggedInUser(), true))
                    result.add(new QuickFilterDto(quickFilter, quickFilter.getCreatorKey().equals(user.getKey()), favouriteQuickFilters != null && favouriteQuickFilters.contains(quickFilter)));
                return result;
            }
        }.getResponse();
    }
}
