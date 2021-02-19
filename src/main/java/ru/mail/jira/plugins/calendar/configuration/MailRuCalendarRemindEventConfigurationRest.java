package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.xsrf.XsrfRequestValidator;
import ru.mail.jira.plugins.calendar.service.PluginData;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/configuration/remindEvent")
@Produces(MediaType.APPLICATION_JSON)
public class MailRuCalendarRemindEventConfigurationRest {
    private final PluginData pluginData;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public MailRuCalendarRemindEventConfigurationRest(PluginData pluginData,
                                                      @ComponentImport GlobalPermissionManager globalPermissionManager,
                                                      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
        this.pluginData = pluginData;
        this.globalPermissionManager = globalPermissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
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
                if (!isAdministrator(jiraAuthenticationContext.getLoggedInUser())) {
                    throw new SecurityException("User doesn't have admin permissions");
                }
                pluginData.setEventIdForRemind(eventId);
                return null;
            }
        }.getResponse();
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
