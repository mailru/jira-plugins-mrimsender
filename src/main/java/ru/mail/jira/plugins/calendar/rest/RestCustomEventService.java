package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.rest.dto.*;
import ru.mail.jira.plugins.calendar.service.CustomEventService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Scanned
@Path("/customEvent")
@Produces(MediaType.APPLICATION_JSON)
public class RestCustomEventService {
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final CustomEventService customEventService;
    private final LicenseService licenseService;

    public RestCustomEventService(
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        CustomEventService customEventService,
        LicenseService licenseService
    ) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.customEventService = customEventService;
        this.licenseService = licenseService;
    }

    @POST
    public Response createCustomEvent(CustomEventDto customEventDto) {
        return new RestExecutor<EventDto>() {
            @Override
            protected EventDto doAction() throws Exception {
                licenseService.checkLicense();
                customEventDto.setEditMode(EditMode.SINGLE_EVENT);
                return customEventService.createEvent(jiraAuthenticationContext.getLoggedInUser(), customEventDto);
            }
        }.getResponse();
    }

    @GET
    @Path("{eventId}")
    public Response getCustomEvent(@PathParam("eventId") int eventId) {
        return new RestExecutor<CustomEventDto>() {
            @Override
            protected CustomEventDto doAction() throws Exception {
                return customEventService.getEventDto(jiraAuthenticationContext.getLoggedInUser(), eventId);
            }
        }.getResponse();
    }

    @PUT
    @Path("{eventId}")
    public Response editCustomEvent(@PathParam("eventId") int eventId, CustomEventDto eventDto) {
        return new RestExecutor<EventDto>() {
            @Override
            protected EventDto doAction() throws Exception {
                eventDto.setId(eventId);
                if (eventDto.getEditMode() == null) {
                    eventDto.setEditMode(EditMode.SINGLE_EVENT);
                }
                return customEventService.editEvent(jiraAuthenticationContext.getLoggedInUser(), eventDto);
            }
        }.getResponse();
    }

    @DELETE
    @Path("{eventId}")
    public Response deleteCustomEvent(@PathParam("eventId") int eventId) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                licenseService.checkLicense();
                customEventService.deleteEvent(jiraAuthenticationContext.getLoggedInUser(), eventId);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("{eventId}/move")
    public Response moveEvent(@PathParam("eventId") final int eventId, EventMoveDto moveDto) {
        return new RestExecutor<EventDto>() {
            @Override
            protected EventDto doAction() throws Exception {
                licenseService.checkLicense();
                if (moveDto.getEditMode() == null) {
                    moveDto.setEditMode(EditMode.SINGLE_EVENT);
                }
                return customEventService.moveEvent(jiraAuthenticationContext.getLoggedInUser(), eventId, moveDto);
            }
        }.getResponse();
    }

    @POST
    @Path("/type")
    public Response createType(EventTypeDto eventTypeDto) {
        return new RestExecutor<EventTypeDto>() {
            @Override
            protected EventTypeDto doAction() throws Exception {
                licenseService.checkLicense();
                return customEventService.createEventType(jiraAuthenticationContext.getLoggedInUser(), eventTypeDto);
            }
        }.getResponse();
    }

    @PUT
    @Path("/type/{typeId}")
    public Response createType(@PathParam("typeId") int typeId, EventTypeDto eventTypeDto) {
        return new RestExecutor<EventTypeDto>() {
            @Override
            protected EventTypeDto doAction() throws Exception {
                licenseService.checkLicense();
                eventTypeDto.setId(typeId);
                return customEventService.editEventType(jiraAuthenticationContext.getLoggedInUser(), eventTypeDto);
            }
        }.getResponse();
    }

    @DELETE
    @Path("/type/{typeId}")
    public Response deleteType(@PathParam("typeId") int typeId) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                licenseService.checkLicense();
                customEventService.deleteEventType(jiraAuthenticationContext.getLoggedInUser(), typeId);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("/type/list")
    public Response searchType(@QueryParam("calendarId") int calendarId) {
        return new RestExecutor<List<EventTypeDto>>() {
            @Override
            protected List<EventTypeDto> doAction() throws Exception {
                return customEventService.getTypes(jiraAuthenticationContext.getLoggedInUser(), calendarId);
            }
        }.getResponse();
    }

    @GET
    @Path("/eventCount")
    public Response getEventCount(@QueryParam("calendarId") int calendarId) {
        return new RestExecutor<CountDto>() {
            @Override
            protected CountDto doAction() throws Exception {
                return new CountDto(customEventService.getEventCount(jiraAuthenticationContext.getLoggedInUser(), calendarId));
            }
        }.getResponse();
    }
}
