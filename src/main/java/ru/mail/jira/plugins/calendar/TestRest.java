package ru.mail.jira.plugins.calendar;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.project.ProjectAssigneeTypes;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Share;
import ru.mail.jira.plugins.calendar.model.UserData;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Path("/runner")
public class TestRest {
    private final String PLUGIN_KEY = "SimpleCalendar";
    private final String CALENDARS = "calendars";
    private static final String CALENDARS_HAVE_BEEN_MIGRATED_KEY = "chbm";

    private final ActiveObjects ao;
    private final CalendarMigrator migrator;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserPreferenceMigrator userPreferenceMigrator;
    private final ProjectService projectService;

    private final static Logger log = LoggerFactory.getLogger(TestRest.class);

    public TestRest(ActiveObjects ao, CalendarMigrator migrator, JiraAuthenticationContext jiraAuthenticationContext, PluginSettingsFactory pluginSettingsFactory, UserPreferenceMigrator userPreferenceMigrator, ProjectService projectService) {
        this.ao = ao;
        this.migrator = migrator;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userPreferenceMigrator = userPreferenceMigrator;
        this.projectService = projectService;
    }

    @GET
    @Path("deleteAll")
    public Response deleteAll() {
        log.info("delete from rest");
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                Share[] shares = ao.find(Share.class);
                for (Share share: shares)
                    ao.delete(share);

                Calendar[] calendars = ao.find(Calendar.class);
                for (Calendar calendar: calendars)
                    ao.delete(calendar);

                UserData[] userDatas = ao.find(UserData.class);
                for (UserData userData: userDatas)
                    ao.delete(userData);

                return null;
            }
        });
        return Response.ok().build();
    }


    @GET
    @Path("calendar")
    public Response getCalendar(@QueryParam("id") int calendarId) {
        StringBuilder result = new StringBuilder();
        Calendar calendar = ao.get(Calendar.class, calendarId);
        if (calendar == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("can not find calendar").build();

        result.append("Name => ").append(calendar.getName())
                .append("<br>auhtor => ").append(calendar.getAuthorKey())
                .append("<br>Source => ").append(calendar.getSource())
                .append("<br>Start => ").append(calendar.getEventStart())
                .append("<br>End => ").append(calendar.getEventEnd());
        return Response.ok().entity(result.toString()).build();
    }

    @GET
    @Path("checkFlag")
    public Response checkFlag() {
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        String result = (String) pluginSettings.get(CALENDARS_HAVE_BEEN_MIGRATED_KEY);
        return Response.ok().entity(result).build();
    }

    @GET
    @Path("clearFlag")
    public Response clearFlag() {
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        pluginSettings.remove(CALENDARS_HAVE_BEEN_MIGRATED_KEY);
        return Response.ok().build();
    }

    @GET
    @Path("migrate")
    public Response migrate() {
        try {
            log.info("migrate from rest");
            Map<Long,Integer> oldToNewCalendarIds = migrator.migrateCalendars();
//            userPreferenceMigrator.migrate(oldToNewCalendarIds);
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("calendarList")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCalendarIdList() {
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        Object calendarsSetting = pluginSettings.get(CALENDARS);
        List<Long> result = calendarsSetting == null ? new ArrayList<Long>(0) : strToListLongs((String) calendarsSetting);
        return Response.ok().entity(result).build();
    }

    @GET
    @Path("oldCalendar")
    public Response getOldCalendar(@QueryParam("id") long id) {
        String calendarPluginSettgingKey = calKey(id);
        PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
        String xml = (String) pluginSettings.get(calendarPluginSettgingKey);
        return Response.ok().entity(xml).build();
    }

    @GET
    @Path("clearOldData")
    public Response deleteOldCalendars() {
        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
            Object calendarsSetting = pluginSettings.get(CALENDARS);
            if (calendarsSetting != null) {
                for (Long oldCalendarId: strToListLongs((String) calendarsSetting)) {
                    String calendarPluginSettgingKey = calKey(oldCalendarId);
                    pluginSettings.remove(calendarPluginSettgingKey);
                }
            }
            pluginSettings.remove(CALENDARS);
            return Response.ok("Done").build();
        } catch (Exception e) {
            log.error("Error while trying to delete old calendar data", e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    private String calKey(Long calId) {
        return (calId + ".data");
    }

    public static List<Long> strToListLongs(String source) {
        List<Long> list = new ArrayList<Long>();
        if (StringUtils.isEmpty(source))
            return list;

        for (String str : source.split("&")) {
            try {
                list.add(Long.valueOf(str));
            } catch (NumberFormatException ignored) {
                //ignore
            }
        }
        return list;
    }

    @GET
    @Path("testCreateProject")
    public Response createProjects() {
        for (int k = 0; k < 7; k++) {
            for (int j = 0; j < 25; j++) {
                for (int i = 0; i < 25; i++) {
                    String key = String.format("%c%c%c", 65 + k, 65 + i, 65 + j);
                    ProjectService.CreateProjectValidationResult createProjectValidationResult =
                            projectService.validateCreateProject(jiraAuthenticationContext.getLoggedInUser(),
                                    "PN" + key, key, "Some descr", "admin", "http://localhost:2990/jira/projects/" + key, ProjectAssigneeTypes.UNASSIGNED);
                    if (createProjectValidationResult.isValid())
                        projectService.createProject(createProjectValidationResult);
                }
            }
        }
        return Response.ok().build();
    }
}
