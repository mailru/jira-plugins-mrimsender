package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.order.SortOrder;
import ru.mail.jira.plugins.calendar.planning.PlanningService;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanForm;
import ru.mail.jira.plugins.calendar.service.Order;
import ru.mail.jira.plugins.calendar.service.gantt.GanttParams;
import ru.mail.jira.plugins.calendar.service.gantt.GanttService;
import ru.mail.jira.plugins.calendar.service.applications.JiraSoftwareHelper;
import ru.mail.jira.plugins.calendar.service.applications.SprintDto;
import ru.mail.jira.plugins.calendar.service.gantt.SprintSearcher;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Scanned
@Path("/gantt")
@Produces(MediaType.APPLICATION_JSON)
public class GanttResource {
    private final JiraAuthenticationContext authenticationContext;
    private final SprintSearcher sprintSearcher;
    private final GanttService ganttService;
    private final PlanningService planningService;
    private final JiraSoftwareHelper jiraSoftwareHelper;

    public GanttResource(
        @ComponentImport JiraAuthenticationContext authenticationContext,
        SprintSearcher sprintSearcher,
        GanttService ganttService,
        PlanningService planningService,
        JiraSoftwareHelper jiraSoftwareHelper
    ) {
        this.authenticationContext = authenticationContext;
        this.sprintSearcher = sprintSearcher;
        this.ganttService = ganttService;
        this.planningService = planningService;
        this.jiraSoftwareHelper = jiraSoftwareHelper;
    }

    @GET
    @Path("{id}")
    public Response loadGantt(
        @PathParam("id") int calendarId,
        @QueryParam("start") String startDate,
        @QueryParam("end") String endDate,
        @QueryParam("groupBy") String groupBy,
        @QueryParam("orderBy") String orderBy,
        @QueryParam("order") SortOrder sortOrder,
        @QueryParam("sprint") Long sprintId,
        @QueryParam("fields") List<String> fields,
        @QueryParam("withUnscheduled") boolean withUnscheduled
    ) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                GanttParams params = new GanttParams(getOrder(orderBy, sortOrder), groupBy, sprintId, fields, withUnscheduled, false);

                //if sprint is specified, get all issues without date restrictions
                if (sprintId != null) {
                    return ganttService.getGantt(authenticationContext.getLoggedInUser(), calendarId, params);
                }

                return ganttService.getGantt(authenticationContext.getLoggedInUser(), calendarId, startDate, endDate, params);
            }
        }.getResponse();
    }

    @GET
    @Path("/{id}/optimized")
    public Response loadOptimized(
        @PathParam("id") final int calendarId,
        @QueryParam("deadline") String deadline,
        @QueryParam("groupBy") String groupBy,
        @QueryParam("orderBy") String orderBy,
        @QueryParam("sprint") Long sprintId,
        @QueryParam("fields") List<String> fields
    ) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                return planningService.doPlan(
                    authenticationContext.getLoggedInUser(), calendarId, deadline,
                    new GanttParams(new Order(orderBy, null), groupBy, sprintId, fields, true, true)
                );
            }
        }.getResponse();
    }

    @POST
    @Path("{id}/link")
    public Response createLink(
        @PathParam("id") final int calendarId,
        GanttLinkForm form
    ) {
        return new RestExecutor<GanttLinkDto>() {
            @Override
            protected GanttLinkDto doAction() throws GetException {
                return ganttService.createLink(authenticationContext.getLoggedInUser(), calendarId, form);
            }
        }.getResponse();
    }

    @DELETE
    @Path("{id}/link/{linkId}")
    public Response deleteLink(
        @PathParam("id") final int calendarId,
        @PathParam("linkId") final int linkId
    ) {
        return new RestExecutor<GanttActionResponse>() {
            @Override
            protected GanttActionResponse doAction() throws GetException {
                ganttService.deleteLink(authenticationContext.getLoggedInUser(), calendarId, linkId);
                return new GanttActionResponse<>(GanttActionResponse.Action.deleted, null);
            }
        }.getResponse();
    }

    @PUT
    @Path("{id}/task/{issueKey}")
    public Response updateTask(
        @PathParam("id") final int calendarId,
        @PathParam("issueKey") final String issueKey,
        @QueryParam("fields") List<String> fields,
        GanttTaskForm form
    ) {
        //todo: return object with list field
        return new RestExecutor<List<GanttTaskDto>>() {
            @Override
            protected List<GanttTaskDto> doAction() throws Exception {
                return ganttService.updateDates(authenticationContext.getLoggedInUser(), calendarId, issueKey, form, fields);
            }
        }.getResponse();
    }

    @POST
    @Path("{id}/task/{issueKey}/estimate")
    public Response updateTask(
        @PathParam("id") final int calendarId,
        @PathParam("issueKey") final String issueKey,
        @QueryParam("fields") List<String> fields,
        GanttEstimateForm form
    ) {
        return new RestExecutor<GanttTaskDto>() {
            @Override
            protected GanttTaskDto doAction() throws Exception {
                return ganttService.setEstimate(authenticationContext.getLoggedInUser(), calendarId, issueKey, form, fields);
            }
        }.getResponse();
    }

    @POST
    @Path("/{id}/applyPlan")
    public Response applyPlan(
        @PathParam("id") int calendarId,
        GanttPlanForm form
    ) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                ganttService.applyPlan(authenticationContext.getLoggedInUser(), calendarId, form);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("/sprints")
    public Response findSprints(@QueryParam("query") String query) {
        return new RestExecutor<List<SprintDto>>() {
            @Override
            protected List<SprintDto> doAction() {
                return jiraSoftwareHelper.findSprints(authenticationContext.getLoggedInUser(), query);
            }
        }.getResponse();
    }

    @GET
    @Path("/calendarSprints/{id}")
    public Response findCalendarSprints(@PathParam("id") int calendarId) {
        return new RestExecutor<List<SprintDto>>() {
            @Override
            protected List<SprintDto> doAction() throws GetException, SearchException {
                ApplicationUser user = authenticationContext.getLoggedInUser();
                return sprintSearcher.findSprintsForCalendar(user, calendarId);
            }
        }.getResponse();
    }

    @GET
    @Path("/errors/{id}")
    public Response getErrors(@PathParam("id") int calendarId) {
        return new RestExecutor<List<String>>() {
            @Override
            protected List<String> doAction() throws GetException, SearchException {
                ApplicationUser user = authenticationContext.getLoggedInUser();
                return ganttService.getErrors(user, calendarId);
            }
        }.getResponse();
    }

    private Order getOrder(String orderBy, SortOrder sortOrder) {
        if (orderBy != null && sortOrder != null) {
            return new Order(orderBy, sortOrder);
        }
        return null;
    }
}
