package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.order.SortOrder;
import ru.mail.jira.plugins.calendar.planning.PlanningService;
import ru.mail.jira.plugins.calendar.rest.dto.SingleValueDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanForm;
import ru.mail.jira.plugins.calendar.service.GanttService;
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
    private final GanttService ganttService;
    private final PlanningService planningService;

    public GanttResource(
        @ComponentImport JiraAuthenticationContext authenticationContext,
        GanttService ganttService,
        PlanningService planningService
    ) {
        this.authenticationContext = authenticationContext;
        this.ganttService = ganttService;
        this.planningService = planningService;
    }

    @GET
    @Path("{id}")
    public Response loadGantt(
        @PathParam("id") int calendarId,
        @QueryParam("start") String startDate,
        @QueryParam("end") String endDate,
        @QueryParam("groupBy") String groupBy,
        @QueryParam("orderBy") String orderBy,
        @QueryParam("order") SortOrder order,
        @QueryParam("fields") List<String> fields
    ) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                return ganttService.getGantt(authenticationContext.getLoggedInUser(), calendarId, startDate, endDate, groupBy, orderBy, order, fields);
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
        @QueryParam("fields") List<String> fields
    ) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                return planningService.doPlan(authenticationContext.getLoggedInUser(), calendarId, deadline, groupBy, orderBy, fields);
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
        GanttTaskForm updateDto
    ) {
        //todo: return object with list field
        return new RestExecutor<List<GanttTaskDto>>() {
            @Override
            protected List<GanttTaskDto> doAction() throws Exception {
                return ganttService.updateDates(authenticationContext.getLoggedInUser(), calendarId, issueKey, updateDto.getStartDate(), updateDto.getEndDate());
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
                ganttService.applyPlan(
                    authenticationContext.getLoggedInUser(),
                    calendarId,
                    form
                );
                return null;
            }
        }.getResponse();
    }
}
