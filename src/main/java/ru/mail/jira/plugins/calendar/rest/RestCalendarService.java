package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.util.Uris;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.Migrator;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Share;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarFieldsOutput;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarOutput;
import ru.mail.jira.plugins.calendar.rest.dto.Event;
import ru.mail.jira.plugins.calendar.rest.dto.SelectedShare;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.UserDataService;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.annotation.Nullable;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class RestCalendarService {
    private final static Logger log = LoggerFactory.getLogger(RestCalendarService.class);

    private final static String PLUGIN_KEY = "SimpleCalendar";
    private static final String CALENDARS_HAVE_BEEN_MIGRATED_KEY = "chbm";

    private final CalendarService calendarService;
    private final CalendarEventService calendarEventService;

    private final AvatarService avatarService;
    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final I18nHelper i18nHelper;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final Migrator migrator;
    private final PermissionManager permissionManager;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final SearchRequestService searchRequestService;
    private final UserManager userManager;
    private final UserDataService userDataService;

    public RestCalendarService(CalendarService calendarService,
                               CalendarEventService calendarEventService,
                               AvatarService avatarService,
                               GlobalPermissionManager globalPermissionManager,
                               GroupManager groupManager,
                               I18nHelper i18nHelper,
                               JiraAuthenticationContext jiraAuthenticationContext,
                               Migrator migrator,
                               UserManager userManager,
                               PluginSettingsFactory pluginSettingsFactory,
                               ProjectManager projectManager,
                               ProjectRoleManager projectRoleManager,
                               PermissionManager permissionManager,
                               SearchRequestService searchRequestService,
                               UserDataService userDataService) {
        this.calendarService = calendarService;
        this.calendarEventService = calendarEventService;
        this.avatarService = avatarService;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.i18nHelper = i18nHelper;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.migrator = migrator;
        this.userManager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.permissionManager = permissionManager;
        this.searchRequestService = searchRequestService;
        this.userDataService = userDataService;
    }

    private String getUserAvatarSrc(String userKey) {
        return avatarService.getAvatarURL(jiraAuthenticationContext.getUser(), userManager.getUserByKey(userKey), Avatar.Size.SMALL).toString();
    }

    //todo need refactoring
    private synchronized void checkOldCalendars() {
        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY);
            if (pluginSettings.get(CALENDARS_HAVE_BEEN_MIGRATED_KEY) == null) {
                log.info("Calendar have not been migrated");
                migrator.migrate();
                pluginSettings.put(CALENDARS_HAVE_BEEN_MIGRATED_KEY, "migrated");
            } else {
                log.info("Calendar have been migrated earlier");
            }
        } catch (Exception e) {
            log.error("Error while trying to check old calendars", e);
        }
    }

    private void fillSelectedSourceFields(ApplicationUser user, CalendarFieldsOutput output, Calendar calendar) {
        String source = calendar.getSource();
        output.setSelectedSourceId(source);
        if (source.startsWith("project_")) {
            long projectId = Long.parseLong(source.substring("project_".length()));
            Project project = projectManager.getProjectObj(projectId);
            if (project == null || !permissionManager.hasPermission(Permissions.BROWSE, project, user, false)) {
                output.setSelectedSourceIsUnavailable(true);
                output.setSelectedSourceName(i18nHelper.getText("ru.mail.jira.plugins.calendar.unavailableSource"));
            } else {
                output.setSelectedSourceName(String.format("%s (%s)", project.getName(), project.getKey()));
                output.setSelectedSourceAvatarId(project.getAvatar().getId());
            }
        } else if (source.startsWith("filter_")) {
            long filterId = Long.parseLong(source.substring("filter_".length()));
            JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
            SearchRequest filter = searchRequestService.getFilter(serviceContext, filterId);
            if (filter == null) {
                output.setSelectedSourceIsUnavailable(true);
                output.setSelectedSourceName(i18nHelper.getText("ru.mail.jira.plugins.calendar.unavailableSource"));
            } else
                output.setSelectedSourceName(filter.getName());
        } else { // theoretically it isn't possible
            output.setSelectedSourceName("Unknown source");
        }
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    private List<SelectedShare> getSelectedShares(ApplicationUser user, Share[] shares) {
        boolean isAdministrator = isAdministrator(user);
        List<SelectedShare> result = new ArrayList<SelectedShare>(shares.length);
        for (Share share : shares) {
            SelectedShare selectedShare;
            if (StringUtils.isNotEmpty(share.getGroup())) {
                selectedShare = new SelectedShare(share.getGroup());
                if (!groupManager.groupExists(share.getGroup())) {
                    log.warn(String.format("Error whle trying to get share. Couldn't find group with name => %s. Share will be deleted.", share.getGroup()));
                    calendarService.deleteShare(share);
                    continue;
                }
                if (!isAdministrator && !groupManager.isUserInGroup(user.getName(), share.getGroup()))
                    selectedShare.setError(i18nHelper.getText("common.sharing.exception.not.in.group", share.getGroup()));
            } else {
                Project project = projectManager.getProjectObj(share.getProject());
                if (project == null) {
                    log.warn(String.format("Error whle trying to get share. Couldn't find project with id => %s. Share will be deleted.", share.getProject()));
                    calendarService.deleteShare(share);
                    continue;
                }

                ProjectRole projectRole = null;
                if (share.getRole() > 0) {
                    projectRole = projectRoleManager.getProjectRole(share.getRole());
                    if (projectRole == null) {
                        log.warn(String.format("Error whle trying to get share. Couldn't find project role with id => %s. Share will be deleted.", share.getRole()));
                        calendarService.deleteShare(share);
                        continue;
                    }
                }

                selectedShare = new SelectedShare(share.getProject(), project.getName(), share.getRole(), projectRole == null ? "" : projectRole.getName());

                if (!isAdministrator && !permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false))
                    selectedShare.setError(i18nHelper.getText("common.sharing.exception.no.permission.project", project.getName()));

                if (projectRole != null)
                    if (!isAdministrator && !projectRoleManager.isUserInProjectRole(user, projectRole, project))
                        selectedShare.setError(i18nHelper.getText("common.sharing.exception.no.permission.role", project.getName(), projectRole.getName()));
            }
            result.add(selectedShare);
        }

        return result;
    }

    private CalendarOutput buildCalendarOutput(ApplicationUser user,
                                               Calendar calendar,
                                               boolean changable,
                                               boolean visible,
                                               boolean isMy,
                                               boolean fromOthers) {
        CalendarOutput output = new CalendarOutput(calendar);
        output.setChangable(changable);
        output.setVisible(visible);
        output.setIsMy(isMy);
        output.setFromOthers(fromOthers);

        String filterHasNotAvailableError = checkThatFilterHasAvailable(user, calendar);
        if (filterHasNotAvailableError != null) {
            output.setHasError(true);
            output.setError(filterHasNotAvailableError);
        }
        return output;
    }

    @Nullable
    private String checkThatFilterHasAvailable(ApplicationUser user, Calendar calendar) {
        if (calendar.getSource().startsWith("filter_")) {
            JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(user);
            searchRequestService.getFilter(jiraServiceContext, Long.valueOf(calendar.getSource().substring("filter_".length())));
            return jiraServiceContext.getErrorCollection().hasAnyErrors() ? CommonUtils.formatErrorCollection(jiraServiceContext.getErrorCollection()) : null;
        }
        return null;
    }

    @GET
    @Path("{calendarId}")
    public Response getCalendar(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<CalendarFieldsOutput>() {
            @Override
            protected CalendarFieldsOutput doAction() throws Exception {
                CalendarFieldsOutput result = new CalendarFieldsOutput();

                Calendar calendar = calendarService.getCalendar(calendarId);
                ApplicationUser user = userManager.getUserByKey(calendar.getAuthorKey());

                if (user == null) {
                    result.setOwnerFullName("Deleted");
                } else {
                    result.setOwner(calendar.getAuthorKey());
                    result.setOwnerFullName(user.getDisplayName());
                    result.setOwnerAvatarUrl(getUserAvatarSrc(calendar.getAuthorKey()));
                }

                result.setSelectedName(calendar.getName());
                result.setSelectedColor(calendar.getColor());
                result.setSelectedEventStartId(calendar.getEventStart());
                result.setSelectedEventEndId(calendar.getEventEnd());

                if (StringUtils.isNotEmpty(calendar.getDisplayedFields()))
                    result.setSelectedDisplayedFields(Arrays.asList(calendar.getDisplayedFields().split(",")));

                fillSelectedSourceFields(user, result, calendar);

                ApplicationUser currentUser = jiraAuthenticationContext.getUser();
                Share[] shares = calendar.getShares();
                if (shares.length > 0) {
                    List<SelectedShare> selectedShares = getSelectedShares(currentUser, shares);
                    result.setSelectedShares(selectedShares);

                    result.setGroups(userDataService.getUserGroups(user));
                    result.setProjectsForShare(userDataService.getUserProjects(user));

                    Map<Long, Map<Long, String>> projectRolesForShare = new LinkedHashMap<Long, Map<Long, String>>();
                    for (SelectedShare share : selectedShares)
                        if (share.getProjectId() != 0)
                            projectRolesForShare.put(share.getProjectId(), userDataService.getUserRoles(jiraAuthenticationContext.getUser(), share.getProjectId()));

                    result.setProjectRolesForShare(projectRolesForShare);
                }
                return result;
            }
        }.getResponse();
    }

    @POST
    public Response createCalendar(@FormParam("name") final String name,
                                   @FormParam("color") final String color,
                                   @FormParam("source") final String source,
                                   @FormParam("eventStart") final String eventStart,
                                   @FormParam("eventEnd") final String eventEnd,
                                   @FormParam("displayedFields") final String displayedFields,
                                   @FormParam("shares") final String shares) {
        return new RestExecutor<CalendarOutput>() {
            @Override
            protected CalendarOutput doAction() throws Exception {
                CalendarOutput result = new CalendarOutput(
                        calendarService.createCalendar(
                                jiraAuthenticationContext.getUser(),
                                StringUtils.trimToNull(name),
                                StringUtils.trimToNull(source),
                                StringUtils.trimToNull(color),
                                StringUtils.trimToNull(eventStart),
                                StringUtils.trimToNull(eventEnd),
                                StringUtils.trimToNull(displayedFields),
                                StringUtils.trimToNull(shares)));

                result.setChangable(true);
                result.setIsMy(true);
                result.setFromOthers(false);
                result.setVisible(true);
                return result;
            }
        }.getResponse();
    }

    @PUT
    @Path("{id}")
    public Response updateCalendar(@PathParam("id") final int calendarId,
                                   @FormParam("name") final String name,
                                   @FormParam("color") final String color,
                                   @FormParam("source") final String source,
                                   @FormParam("eventStart") final String eventStart,
                                   @FormParam("eventEnd") final String eventEnd,
                                   @FormParam("displayedFields") final String displayedFields,
                                   @FormParam("shares") final String shares) {
        return new RestExecutor<CalendarOutput>() {
            @Override
            protected CalendarOutput doAction() throws Exception {
                ApplicationUser user = jiraAuthenticationContext.getUser();
                Calendar calendar = calendarService.updateCalendar(user,
                                                                   calendarId,
                                                                   StringUtils.trimToNull(name),
                                                                   StringUtils.trimToNull(source),
                                                                   StringUtils.trimToNull(color),
                                                                   StringUtils.trimToNull(eventStart),
                                                                   StringUtils.trimToNull(eventEnd),
                                                                   StringUtils.trimToNull(displayedFields),
                                                                   StringUtils.trimToNull(shares));
                CalendarOutput output = new CalendarOutput(calendar);
                String filterHasNotAvailableError = checkThatFilterHasAvailable(user, calendar);
                if (filterHasNotAvailableError != null) {
                    output.setHasError(true);
                    output.setError(filterHasNotAvailableError);
                }
                output.setVisible(userDataService.isCalendarShowedForCurrentUser(user, calendar));
                return output;
            }
        }.getResponse();
    }

    @DELETE
    @Path("{id}")
    public Response deleteCalendar(@PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarService.deleteCalendar(jiraAuthenticationContext.getUser(), id);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("all")
    public Response getAllCalendars() {
        checkOldCalendars();
        return new RestExecutor<List<CalendarOutput>>() {
            @Override
            protected List<CalendarOutput> doAction() throws Exception {
                List<CalendarOutput> result = new ArrayList<CalendarOutput>();
                final ApplicationUser user = jiraAuthenticationContext.getUser();
                final String userKey = user.getKey();
                final boolean isUserAdmin = globalPermissionManager.hasPermission(Permissions.ADMINISTER, user);

                Set<Integer> showedCalendars = userDataService.getShowedCalendars(user);
                for (Calendar calendar : calendarService.getAllCalendars()) {
                    boolean isShowedCalendar = showedCalendars.contains(calendar.getID());
                    if (calendar.getAuthorKey().equals(userKey)) {
                        result.add(buildCalendarOutput(user, calendar, true, isShowedCalendar, true, false));
                        continue;
                    }

                    boolean sharedToUser = false;
                    Share[] shares = calendar.getShares();
                    if (shares != null) {
                        for (Share share : shares) {
                            if (share.getGroup() != null) {
                                Group group = groupManager.getGroup(share.getGroup());
                                if (group != null && groupManager.isUserInGroup(ApplicationUsers.toDirectoryUser(user), group)) {
                                    result.add(buildCalendarOutput(user, calendar, isUserAdmin, isShowedCalendar, false, false));
                                    sharedToUser = true;
                                    break;
                                }
                            } else {
                                Project project = projectManager.getProjectObj(share.getProject());
                                if (share.getRole() != 0) {
                                    ProjectRole projectRole = projectRoleManager.getProjectRole(share.getRole());
                                    if (projectRole != null && projectRoleManager.isUserInProjectRole(user, projectRole, project)) {
                                        result.add(buildCalendarOutput(user, calendar, isUserAdmin, isShowedCalendar, false, false));
                                        sharedToUser = true;
                                        break;
                                    }
                                } else if (permissionManager.hasPermission(Permissions.BROWSE, project, user, false)) {
                                    result.add(buildCalendarOutput(user, calendar, isUserAdmin, isShowedCalendar, false, false));
                                    sharedToUser = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (isUserAdmin && !sharedToUser)
                        result.add(buildCalendarOutput(user, calendar, true, isShowedCalendar, false, true));
                }
                return result;
            }
        }.getResponse();
    }

    @PUT
    @Path("/{calendarId}/visibility")
    public Response invertVisibility(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<Boolean>() {
            @Override
            protected Boolean doAction() throws Exception {
                return calendarService.invertCalendarVisibility(calendarId, jiraAuthenticationContext.getUser());
            }
        }.getResponse();
    }

    @GET
    @Produces("text/calendar")
    @Path("{icalUid}/{calendars}.ics")
    @AnonymousAllowed
    public Response getIcsCalendar(@PathParam("icalUid") final String icalUid,
                                   @PathParam("calendars") final String calendars) {
        return new RestExecutor<StreamingOutput>() {
            @Override
            protected StreamingOutput doAction() throws Exception {
                String[] calendarIds = StringUtils.split(calendars, "-");

                UserData userData = userDataService.getUserDataByIcalUid(icalUid);
                if (userData == null)
                    return null;

                final net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
                calendar.getProperties().add(new ProdId("-//MailRu Calendar/" + icalUid + "/iCal4j 1.0//EN"));
                calendar.getProperties().add(net.fortuna.ical4j.model.property.Version.VERSION_2_0);
                calendar.getProperties().add(CalScale.GREGORIAN);

                LocalDate startSearch = LocalDate.now().minusMonths(3);
                LocalDate endSearch = LocalDate.now().plusMonths(1);

                for (String calendarId : calendarIds) {
                    List<Event> events = calendarEventService.findEvents(Integer.parseInt(calendarId),
                                                                         startSearch.toString("yyyy-MM-dd"),
                                                                         endSearch.toString("yyyy-MM-dd"),
                                                                         userManager.getUserByKey(userData.getUserKey()),
                                                                         true);

                    org.joda.time.format.DateTimeFormatter clientDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
                    for (Event event : events) {
                        DateTime start = new DateTime(clientDateFormat.parseMillis(event.getStart()));
                        DateTime end = event.getEnd() != null ? new DateTime(clientDateFormat.parseMillis(event.getEnd())) : null;

                        VEvent vEvent = end != null ? new VEvent(start, end, event.getTitle()) : new VEvent(start, event.getTitle());
                        vEvent.getProperties().add(new Uid(calendarId + "_" + event.getId()));
                        vEvent.getProperties().add(new Url(Uris.create(ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/browse/" + event.getId())));
                        if(event.getIssueInfo() != null)
                            vEvent.getProperties().add(new Description(event.getIssueInfo().toFormatString(i18nHelper)));
                        calendar.getComponents().add(vEvent);
                    }
                }

                return new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException, WebApplicationException {
                        try {
                            new CalendarOutputter().output(calendar, output);
                        } catch (ValidationException e) {
                            throw new IOException(e);
                        }
                    }
                };
            }
        }.getResponse();
    }
}
