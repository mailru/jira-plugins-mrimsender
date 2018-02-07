package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import ru.mail.jira.plugins.calendar.rest.dto.GanttDto;
import ru.mail.jira.plugins.calendar.service.GanttService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    public Response createLink(@PathParam("id") final int calendarId,
                               @FormParam("source") final String sourceKey,
                               @FormParam("target") final String targetKey,
                               @FormParam("type") final String type) {
        return new RestExecutor<Integer>() {
            @Override
            protected Integer doAction() throws Exception {
                return ganttService.createLink(calendarId, sourceKey, targetKey, type).getID();
            }
        }.getResponse();
    }


    @POST
    @Path("{id}/task/{issueKey}")
    public Response updateIssueDates(@PathParam("id") final int calendarId,
                                     @PathParam("issueKey") final String issueKey,
                                     @FormParam("start_date") final String startDate,
                                     @FormParam("end_date") final String endDate) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                ganttService.updateDates(calendarId, issueKey, startDate, endDate);
                return null;
            }
        }.getResponse();
    }
}
