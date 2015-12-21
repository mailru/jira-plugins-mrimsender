package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.UserPreferences;
import ru.mail.jira.plugins.calendar.service.UserDataService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/calendar/userPreference")
@Produces(MediaType.APPLICATION_JSON)
public class RestUserPreferenceService {
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserDataService userDataService;

    public RestUserPreferenceService(JiraAuthenticationContext jiraAuthenticationContext, UserDataService userDataService) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userDataService = userDataService;
    }

    @GET
    public Response getUserPreference() {
        return new RestExecutor<UserPreferences>() {
            @Override
            protected UserPreferences doAction() throws Exception {
                return new UserPreferences(userDataService.getUserData(jiraAuthenticationContext.getUser()));
            }
        }.getResponse();
    }

    @PUT
    @Path("view")
    public Response updateUserDefaultView(@QueryParam("value") final String view) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                userDataService.updateUserData(jiraAuthenticationContext.getUser(), view, null);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("hideWeekends")
    public Response updateUserHideWeekendsOption() {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getUser();
                UserData userData = userDataService.getUserData(user);
                userDataService.updateUserData(user, null, !(userData != null && userData.isHideWeekends()));
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("favorite")
    public Response addFavoriteCalendars(@FormParam("calendars") final List<Integer> calendars) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                userDataService.updateFavorites(jiraAuthenticationContext.getUser(), calendars);
                return null;
            }
        }.getResponse();
    }

    @DELETE
    @Path("favorite/{calendarId}")
    public Response removeFavoriteCalendars(@PathParam("calendarId") final Integer calendarId) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                userDataService.removeFavorite(jiraAuthenticationContext.getUser(), calendarId);
                return null;
            }
        }.getResponse();
    }

    @POST
    @Path("ics/feed")
    public Response updateCalendarFeedUrl() {
        return new RestExecutor<UserPreferences>() {
            @Override
            protected UserPreferences doAction() throws Exception {
                return new UserPreferences(userDataService.updateIcalUid(jiraAuthenticationContext.getUser()));
            }
        }.getResponse();
    }

    @GET
    @Path("groups")
    public Response getGroupsForShare() {
        return new RestExecutor<List<String>>() {
            @Override
            protected List<String> doAction() throws Exception {
                return userDataService.getUserGroups(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @GET
    @Path("projects")
    public Response getProjectsForShare() {
        return new RestExecutor<Map<Long, String>>() {
            @Override
            protected Map<Long, String> doAction() throws Exception {
                return userDataService.getUserProjects(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @GET
    @Path("project/{id}/roles")
    public Response getProjectsRolesForShare(@PathParam("id") final long projectId) {
        return new RestExecutor<Map<Long, String>>() {
            @Override
            protected Map<Long, String> doAction() throws Exception {
                return userDataService.getUserRoles(jiraAuthenticationContext.getUser(), projectId);
            }
        }.getResponse();
    }
}
