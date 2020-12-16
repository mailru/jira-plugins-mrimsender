package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.rest.dto.UserDataDto;
import ru.mail.jira.plugins.calendar.service.UserDataService;
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

@Path("/calendar/userPreference")
@Produces(MediaType.APPLICATION_JSON)
public class RestUserPreferenceService {
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserDataService userDataService;
    private final LicenseService licenseService;

    public RestUserPreferenceService(
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        UserDataService userDataService,
        LicenseService licenseService
    ) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userDataService = userDataService;
        this.licenseService = licenseService;
    }

    @GET
    public Response getUserPreference() {
        return new RestExecutor<UserDataDto>() {
            @Override
            protected UserDataDto doAction() throws Exception {
                return userDataService.getUserDataDto(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @PUT
    public Response update(final UserDataDto userDataDto) {
        return new RestExecutor<UserDataDto>() {
            @Override
            protected UserDataDto doAction() throws Exception {
                licenseService.checkLicense();
                return userDataService.updateUserData(jiraAuthenticationContext.getUser(), userDataDto);
            }
        }.getResponse();
    }

    @PUT
    @Path("likeFlagShown/{rated}")
    public Response updateLastLikeFlagShown(@PathParam("rated") final boolean rated) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                licenseService.checkLicense();
                userDataService.updateUserLikeData(jiraAuthenticationContext.getUser(), rated);
                return null;
            }
        }.getResponse();
    }

    @POST
    @Path("ics/feed")
    public Response updateCalendarFeedUrl() {
        return new RestExecutor<UserDataDto>() {
            @Override
            protected UserDataDto doAction() throws Exception {
                licenseService.checkLicense();
                return userDataService.updateIcalUid(jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @DELETE
    @Path("favorite/{calendarId}")
    public Response removeFavoriteCalendars(@PathParam("calendarId") final Integer calendarId) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                licenseService.checkLicense();
                userDataService.removeUserCalendar(jiraAuthenticationContext.getUser(), calendarId);
                return null;
            }
        }.getResponse();
    }
}
