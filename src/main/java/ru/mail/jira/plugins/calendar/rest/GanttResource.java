package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.lang.Pair;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableMap;
import ru.mail.jira.plugins.calendar.planning.PlanningEngine;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.service.GanttService;
import ru.mail.jira.plugins.calendar.service.JiraDeprecatedService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Scanned
@Path("/gantt")
@Produces(MediaType.APPLICATION_JSON)
public class GanttResource {
    private final JiraAuthenticationContext authenticationContext;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final GanttService ganttService;
    private final PlanningEngine planningEngine;

    public GanttResource(
        @ComponentImport JiraAuthenticationContext authenticationContext,
        JiraDeprecatedService jiraDeprecatedService,
        GanttService ganttService,
        PlanningEngine planningEngine
    ) {
        this.authenticationContext = authenticationContext;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.ganttService = ganttService;
        this.planningEngine = planningEngine;
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

    @GET
    @Path("/{id}/optimized")
    public Response loadOptimized(
        @PathParam("id") final int calendarId,
        @QueryParam("start") final String startDate,
        @QueryParam("end") final String endDate
    ) {
        return new RestExecutor<GanttDto>() {
            @Override
            protected GanttDto doAction() throws Exception {
                DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(authenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE);

                GanttDto ganttData = ganttService.getGantt(calendarId, startDate, endDate);

                List<GanttTaskDto> ganttEvents = ganttData.getData();
                List<EventDto> events = ganttEvents
                    .stream()
                    .map(GanttTaskDto::getOriginalEvent)
                    .collect(Collectors.toList());
                List<GanttLinkDto> links = ganttData.getCollections().getLinks();

                Map<EventDto, List<EventDto>> dependencies = new HashMap<>();

                for (GanttLinkDto link : links) {
                    EventDto source = events.stream().filter(event -> event.getId().equals(link.getSource())).findAny().orElse(null);
                    EventDto target = events.stream().filter(event -> event.getId().equals(link.getTarget())).findAny().orElse(null);

                    List<EventDto> list = dependencies.computeIfAbsent(target, (key) -> new ArrayList<>());
                    list.add(source);
                }

                Map<EventDto, Pair<Date, Date>> plan = planningEngine.generatePlan(
                    events,
                    events
                        .stream()
                        .collect(Collectors.toMap(
                            Function.identity(),
                            event -> {
                                if (event.getOriginalEstimateSeconds() != null) {
                                    return (int) TimeUnit.SECONDS.toHours(event.getOriginalEstimateSeconds());
                                }
                                return 8;
                            }
                        )),
                    ImmutableMap.of(), //todo
                    dependencies,
                    ImmutableMap.of(), //todo
                    90,
                    8
                );

                plan.forEach((event, dates) -> {
                    GanttTaskDto ganttTask = ganttEvents.stream().filter(e -> e.getId().equals(event.getId())).findAny().orElse(null);

                    ganttTask.setStartDate(dateFormat.format(dates.first()));
                    ganttTask.setEndDate(dateFormat.format(dates.second()));
                    ganttTask.setOverdueSeconds(null);
                    ganttTask.setEarlySeconds(null);
                });

                return ganttData;
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
