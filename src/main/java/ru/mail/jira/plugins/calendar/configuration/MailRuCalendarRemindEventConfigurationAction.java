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

public class MailRuCalendarRemindEventConfigurationAction extends JiraWebActionSupport {
    private final EventTypeManager eventTypeManager;


    public MailRuCalendarRemindEventConfigurationAction(

            @ComponentImport EventTypeManager eventTypeManager) {
        this.eventTypeManager = eventTypeManager;
    }

    public Collection<EventType> getAllEvents() {
        return eventTypeManager.getEventTypes();
    }

    @Override
    public String execute() {
        return SUCCESS;
    }


}
