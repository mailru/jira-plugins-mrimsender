package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTeamDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttUserDto;
import ru.mail.jira.plugins.calendar.service.gantt.GanttTeamService;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Scanned
@Path("/gantt/team")
@Produces(MediaType.APPLICATION_JSON)
public class RestGanttTeamService {
    private final static Logger log = LoggerFactory.getLogger(RestGanttTeamService.class);

    private final GanttTeamService ganttTeamService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final LicenseService licenseService;

    public RestGanttTeamService(
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            GanttTeamService ganttTeamService,
            LicenseService licenseService
    ) {
        this.ganttTeamService = ganttTeamService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.licenseService = licenseService;
    }

    @POST
    public Response createTeam(final GanttTeamDto teamDto) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.createTeam(jiraAuthenticationContext.getLoggedInUser(), teamDto);
            }
        }.getResponse();
    }

    @PUT
    public Response editTeam(final GanttTeamDto teamDto) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.editTeam(jiraAuthenticationContext.getLoggedInUser(), teamDto);
            }
        }.getResponse();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteTeam(@PathParam("id") final int id) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.deleteTeam(jiraAuthenticationContext.getLoggedInUser(), id);
            }
        }.getResponse();
    }

    @GET
    @Path("/all")
    public Response getAllTeams(@QueryParam("calendarId") final int calendarId) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.getTeams(jiraAuthenticationContext.getLoggedInUser(), calendarId);
            }
        }.getResponse();
    }

    @GET
    @Path("/findUsers")
    public Response findUsers(@QueryParam("calendarId") final int calendarId,
                              @QueryParam("filter") final String filter) {
        return new RestExecutor<List<UserDto>>() {
            @Override
            protected List<UserDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.findUsers(jiraAuthenticationContext.getLoggedInUser(), calendarId, filter);
            }
        }.getResponse();
    }

    @POST
    @Path("/{id}/users")
    public Response addUsers(@PathParam("id") final int teamId,
                             final List<UserDto> selectedUsers) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();
                return ganttTeamService.addUsers(jiraAuthenticationContext.getLoggedInUser(), teamId, selectedUsers);
            }
        }.getResponse();
    }

    @DELETE
    @Path("/{id}/user/{userId}")
    public Response deleteUser(@PathParam("id") final int teamId,
                               @PathParam("userId") final int userId) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.deleteUser(jiraAuthenticationContext.getLoggedInUser(), teamId, userId);
            }
        }.getResponse();
    }

    @PUT
    @Path("/{id}/user/{userId}")
    public Response updateUser(@PathParam("id") final int teamId,
                               @PathParam("userId") final int userId,
                               final GanttUserDto ganttUserDto) {
        return new RestExecutor<List<GanttTeamDto>>() {
            @Override
            protected List<GanttTeamDto> doAction() throws Exception {
                licenseService.checkLicense();

                return ganttTeamService.updateUser(jiraAuthenticationContext.getLoggedInUser(), teamId, userId, ganttUserDto);
            }
        }.getResponse();
    }
}
