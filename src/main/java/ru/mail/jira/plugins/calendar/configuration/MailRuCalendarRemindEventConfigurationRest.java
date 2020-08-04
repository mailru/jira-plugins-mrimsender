package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.event.type.EventTypeManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.service.PluginData;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

@Path("/configuration/remindEvent")
@Produces(MediaType.APPLICATION_JSON)
public class MailRuCalendarRemindEventConfigurationRest {
    private final PluginData pluginData;

    public MailRuCalendarRemindEventConfigurationRest(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    @GET
    public Response getRemindEvent() {
        return new RestExecutor<Long>() {
            @Override
            protected Long doAction(){
                long eventIdForRemind = pluginData.getEventIdForRemind();
                return eventIdForRemind;
            }
        }.getResponse();
    }

    @POST
    @Path("{eventId}")
    public Response setRemindEvent(@PathParam("eventId") final long eventId) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                pluginData.setEventIdForRemind(eventId);
                return null;
            }
        }.getResponse();
    }
}
