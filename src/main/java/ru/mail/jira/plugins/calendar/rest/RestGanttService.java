package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.service.GanttService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Scanned
@Path("/gantt")
@Produces(MediaType.APPLICATION_JSON)
public class RestGanttService {
    private final GanttService ganttService;

    public RestGanttService(GanttService ganttService) {
        this.ganttService = ganttService;
    }

    @GET
    @Path("{id}")
    public Response loadGantt(@PathParam("id") final int calendarId,
                              @QueryParam("start") final String startDate,
                              @QueryParam("end") final String endDate) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                return ganttService.getGantt(calendarId, startDate, endDate);
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
            protected GanttLinkDto doAction() {
                return ganttService.createLink(calendarId, form);
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
            protected GanttActionResponse doAction() {
                ganttService.deleteLink(calendarId, linkId);
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
        return new RestExecutor<GanttTaskDto>() {
            @Override
            protected GanttTaskDto doAction() throws Exception {
                return ganttService.updateDates(calendarId, issueKey, updateDto.getStartDate(), updateDto.getEndDate());
            }
        }.getResponse();
    }
}
