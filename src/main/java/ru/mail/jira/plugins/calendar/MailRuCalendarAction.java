package ru.mail.jira.plugins.calendar;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.*;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/calendar")
@Produces(MediaType.APPLICATION_JSON)
public class MailRuCalendarAction extends JiraWebActionSupport {
    private static final String CREATED_DATE_KEY = "created";
    private static final String UPDATED_DATE_KEY = "updated";
    private static final String RESOLVED_DATE_KEY = "resolved";
    public  static final String DUE_DATE_KEY = "due_date";

    private final static String DATE_RANGE_FORMAT = "yyyy-MM-dd";
    private final static String PLUGIN_KEY = "SimpleCalendar";
    private static final String CALENDARS_HAVE_BEEN_MIGRATED_KEY = "chbm";
    private final int MILLIS_IN_DAY = 86400000;

    private final static Logger log = LoggerFactory.getLogger(MailRuCalendarAction.class);

    private final CalendarManager calendarManager;

    private final AvatarService avatarService;
    private final DateTimeFormatter dateTimeFormatter;
    private final CustomFieldManager customFieldManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final I18nHelper i18nHelper;
    private final IssueService issueService;
    private final Migrator migrator;
    private final PermissionManager permissionManager;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final ProjectManager projectManager;
    private final ProjectService projectService;
    private final ProjectRoleManager projectRoleManager;
    private final SearchRequestService searchRequestService;
    private final SearchService searchService;

    public MailRuCalendarAction(CalendarManager calendarManager, AvatarService avatarService, DateTimeFormatter dateTimeFormatter, CustomFieldManager customFieldManager, GlobalPermissionManager globalPermissionManager, GroupManager groupManager, I18nHelper i18nHelper, IssueService issueService, Migrator migrator, PermissionManager permissionManager, PluginSettingsFactory pluginSettingsFactory, ProjectManager projectManager, ProjectService projectService, ProjectRoleManager projectRoleManager,SearchRequestService searchRequestService, SearchService searchService) {
        this.calendarManager = calendarManager;
        this.avatarService = avatarService;
        this.dateTimeFormatter = dateTimeFormatter;
        this.customFieldManager = customFieldManager;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.i18nHelper = i18nHelper;
        this.issueService = issueService;
        this.migrator = migrator;
        this.permissionManager = permissionManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.projectManager = projectManager;
        this.projectService = projectService;
        this.projectRoleManager = projectRoleManager;
        this.searchRequestService = searchRequestService;
        this.searchService = searchService;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @GET
    public Response getCalendarOutput() {
        return Response.ok().build();
    }

    @GET
    @Path("/userPreference")
    public Response getUserPreference() {
        return new RestExecutor<UserPreferences>() {
            @Override
            protected UserPreferences doAction() throws Exception {
                return new UserPreferences(calendarManager.getUserData());
            }
        }.getResponse();
    }

    @PUT
    @Path("/userPreference/view")
    public Response updateUserDefaultView(@QueryParam("value") final String view) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                calendarManager.updateUserData(view, null);
                return null;
            }
        }.getResponse();
    }

    @PUT
    @Path("/userPreference/hideWeekends")
    public Response updateUserHideWeekendsOption() {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                UserData userData = calendarManager.getUserData();
                calendarManager.updateUserData(null, !(userData != null && userData.isHideWeekends()));
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("{calendarId}")
    public Response getCalendar(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<CalendarFieldsOutput>() {
            @Override
            protected CalendarFieldsOutput doAction() throws Exception {
                CalendarFieldsOutput result = new CalendarFieldsOutput();

                Calendar calendar = calendarManager.getCalendar(calendarId);
                ApplicationUser user = getUserManager().getUserByKey(calendar.getAuthorKey());

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

                ApplicationUser currentUser = getLoggedInApplicationUser();
                Share[] shares = calendar.getShares();
                if (shares.length > 0) {
                    List<SelectedShare> selectedShares = getSelectedShares(currentUser, shares);
                    result.setSelectedShares(selectedShares);

                    result.setGroups(getUserGroups());
                    result.setProjectsForShare(getUserProjects());

                    Map<Long,Map<Long,String>> projectRolesForShare = new LinkedHashMap<Long, Map<Long, String>>();
                    for (SelectedShare share: selectedShares)
                        if (share.getProjectId() != 0)
                            projectRolesForShare.put(share.getProjectId(), getUserRoles(share.getProjectId()));

                    result.setProjectRolesForShare(projectRolesForShare);
                }
                return result;
            }
        }.getResponse();
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
                output.setSelectedSourceName(formatProject(project));
                output.setSelectedSourceAvatarId(project.getAvatar().getId());
            }
        } else if (source.startsWith("filter_")) {
            long filterId = Long.parseLong(source.substring("filter_".length()));
            JiraServiceContext serviceContext = new JiraServiceContextImpl(getLoggedInApplicationUser());
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

    private List<SelectedShare> getSelectedShares(ApplicationUser user, Share[] shares) {
        boolean isAdministrator = isAdministrator();
        List<SelectedShare> result = new ArrayList<SelectedShare>(shares.length);
        for (Share share: shares) {
            SelectedShare selectedShare;
            if (StringUtils.isNotEmpty(share.getGroup())) {
                selectedShare = new SelectedShare(share.getGroup());
                if (!groupManager.groupExists(share.getGroup())) {
                    log.warn(String.format("Error whle trying to get share. Couldn't find group with name => %s. Share will be deleted.", share.getGroup()));
                    calendarManager.deleteShare(share);
                    continue;
                }
                if (!isAdministrator && !groupManager.isUserInGroup(user.getName(), share.getGroup()))
                    selectedShare.setError(i18nHelper.getText("common.sharing.exception.not.in.group", share.getGroup()));
            } else {
                Project project = projectManager.getProjectObj(share.getProject());
                if (project == null) {
                    log.warn(String.format("Error whle trying to get share. Couldn't find project with id => %s. Share will be deleted.", share.getProject()));
                    calendarManager.deleteShare(share);
                    continue;
                }

                ProjectRole projectRole = null;
                if (share.getRole() > 0) {
                    projectRole = projectRoleManager.getProjectRole(share.getRole());
                    if (projectRole == null) {
                        log.warn(String.format("Error whle trying to get share. Couldn't find project role with id => %s. Share will be deleted.", share.getRole()));
                        calendarManager.deleteShare(share);
                        continue;
                    }
                }

                selectedShare = new SelectedShare(share.getProject(), project.getName(), share.getRole(), projectRole == null ? "" : projectRole.getName());

                if (!isAdministrator && !permissionManager.hasPermission(Permissions.BROWSE, project, user, false))
                    selectedShare.setError(i18nHelper.getText("common.sharing.exception.no.permission.project", project.getName()));

                if (projectRole != null)
                    if (!isAdministrator && !projectRoleManager.isUserInProjectRole(user, projectRole, project))
                        selectedShare.setError(i18nHelper.getText("common.sharing.exception.no.permission.role", project.getName(), projectRole.getName()));
            }
            result.add(selectedShare);
        }

        return result;
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
                CalendarOutput result = new CalendarOutput(calendarManager.createCalendar(
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
                result.setVisible(false);
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
                Calendar calendar = calendarManager.updateCalendar(calendarId,
                        StringUtils.trimToNull(name),
                        StringUtils.trimToNull(source),
                        StringUtils.trimToNull(color),
                        StringUtils.trimToNull(eventStart),
                        StringUtils.trimToNull(eventEnd),
                        StringUtils.trimToNull(displayedFields),
                        StringUtils.trimToNull(shares));
                ApplicationUser user = getLoggedInApplicationUser();
                CalendarOutput output = new CalendarOutput(calendar);
                String filterHasNotAvailableError = checkThatFilterHasAvailable(user, calendar);
                if (filterHasNotAvailableError != null) {
                    output.setHasError(true);
                    output.setError(filterHasNotAvailableError);
                }
                output.setVisible(calendarManager.isCalendarShowedForCurrentUser(calendar));
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
                calendarManager.deleteCalendar(id);
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
                final ApplicationUser user = getLoggedInApplicationUser();
                final String userKey = user.getKey();
                final boolean isUserAdmin = globalPermissionManager.hasPermission(Permissions.ADMINISTER, user);

                Set<Integer> showedCalendars = getShowedCalendars();
                for (Calendar calendar: calendarManager.getAllCalendars()) {
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

    private CalendarOutput buildCalendarOutput(ApplicationUser user, Calendar calendar, boolean changable, boolean visible, boolean isMy, boolean fromOthers) {
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
            return jiraServiceContext.getErrorCollection().hasAnyErrors() ? formatErrorCollection(jiraServiceContext.getErrorCollection()) : null;
        }
        return null;
    }

    private Set<Integer> getShowedCalendars() {
        UserData userData = calendarManager.getUserData();
        if (userData  != null) {
            String showedCalendars = userData.getShowedCalendars();
            if (StringUtils.isNotEmpty(showedCalendars)) {
                String[] splittedShowedCalendars = showedCalendars.split(";");
                Set<Integer> result = new HashSet<Integer>(splittedShowedCalendars.length);
                for (String calendarIdStr: splittedShowedCalendars)
                    result.add(Integer.parseInt(calendarIdStr));
                return result;
            }
        }
        return new HashSet<Integer>(0);
    }

    @GET
    @Path("groups")
    public Response getGroupsForShare() {
        return new RestExecutor<List<String>>() {
            @Override
            protected List<String> doAction() throws Exception {
                return getUserGroups();
            }
        }.getResponse();
    }

    private List<String> getUserGroups() {
        Collection<Group> groups = isAdministrator()
                ? groupManager.getAllGroups()
                : groupManager.getGroupsForUser(getLoggedInApplicationUser().getName());
        List<String> result = new ArrayList<String>(groups.size());
        for (Group group: groups)
            result.add(group.getName());
        return result;
    }

    @GET
    @Path("projects")
    public Response getProjectsForShare() {
        return new RestExecutor<Map<Long,String>>() {
            @Override
            protected Map<Long,String> doAction() throws Exception {
                return getUserProjects();
            }
        }.getResponse();
    }

    @GET
    @Path("project/{id}/roles")
    public Response getProjectsRolesForShare(@PathParam("id") final long projectId) {
        return new RestExecutor<Map<Long,String>>() {
            @Override
            protected Map<Long,String> doAction() throws Exception {
                return getUserRoles(projectId);
            }
        }.getResponse();
    }

    private Map<Long,String> getUserProjects() {
        Map<Long,String> result = new LinkedHashMap<Long, String>();
        List<Project> allProjects = isAdministrator()
                ? projectManager.getProjectObjects()
                : projectService.getAllProjects(getLoggedInApplicationUser()).get();

        for (Project project : allProjects)
            result.put(project.getId(), project.getName());

        return result;
    }

    private Map<Long,String> getUserRoles(long projectId) {
        Map<Long,String> result = new LinkedHashMap<Long, String>();
        Collection<ProjectRole> projectRoles;
        if (isAdministrator()) {
            projectRoles = projectRoleManager.getProjectRoles();
        } else {
            Project project = projectManager.getProjectObj(projectId);
            projectRoles = projectRoleManager.getProjectRoles(getLoggedInApplicationUser(), project);
        }

        for (ProjectRole role: projectRoles)
            result.put(role.getId(), role.getName());

        return result;
    }

    @GET
    @Path("{calendarId}/events")
    public Response getEvents(@PathParam("calendarId") final int calendarId,
                              @QueryParam("start") final String start,
                              @QueryParam("end") final String end) {
        try {
            List<Event> result = new ArrayList<Event>();
            Calendar calendar = calendarManager.getCalendar(calendarId);
            DateFormat dateFormat = new SimpleDateFormat(DATE_RANGE_FORMAT);
            String source = calendar.getSource();
            if (source.startsWith("project_"))
                result = getProjectEvents(calendar, Long.parseLong(source.substring("project_".length())), calendar.getEventStart(), calendar.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end));
            else if (source.startsWith("filter_"))
                result = getFilterEvents(calendar, Long.parseLong(source.substring("filter_".length())), calendar.getEventStart(), calendar.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end));

            CacheControl casheControl = new CacheControl();
            casheControl.setNoCache(true);
            casheControl.setNoStore(true);
            casheControl.setMaxAge(0);
            return Response.ok(result).cacheControl(casheControl).build();
        } catch (Exception e) {
            log.error("Error while trying to get events", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{calendarId}/event/{eventId}/info")
    public Response getEventInfo(@PathParam("calendarId") final int calendarId, @PathParam("eventId") final String eventId) {
        return new RestExecutor<IssueInfo>() {
            @Override
            protected IssueInfo doAction() throws Exception {
                ApplicationUser user = getLoggedInApplicationUser();
                Calendar calendar = calendarManager.getCalendar(calendarId);
                IssueService.IssueResult issueResult = issueService.getIssue(ApplicationUsers.toDirectoryUser(user), eventId);
                MutableIssue issue = issueResult.getIssue();

                IssueInfo result = new IssueInfo(eventId,
                        issue.getSummary(),
                        StringUtils.isBlank(issue.getDescription()) ? null : issue.getDescription(),
                        issue.getAssignee() == null ? null : issue.getAssignee().getDisplayName(),
                        issue.getReporter() == null ? null : issue.getReporter().getDisplayName());
                result.setStatusColor(issue.getStatusObject().getStatusCategory().getColorName());
                if (StringUtils.isNotEmpty(calendar.getDisplayedFields()))
                    fillDisplayedFields(result, calendar.getDisplayedFields().split(","), issue);

                return result;
            }
        }.getResponse();
    }

    private void fillDisplayedFields(IssueInfo issueInfo, String[] extraFields, Issue issue) {
        for (String extraField: extraFields) {
            if (extraField.equals(CalendarManager.STATUS))
                issueInfo.setStatus(issue.getStatusObject().getName());
            else if (extraField.equals(CalendarManager.LABELS) && issue.getLabels() != null && !issue.getLabels().isEmpty()) {
                List<String> labelList = new ArrayList<String>();
                for (Label label : issue.getLabels())
                    labelList.add(label.getLabel());
                issueInfo.setLabels(labelList.toString());
            } else if (extraField.equals(CalendarManager.COMPONENTS) && issue.getComponentObjects() != null && !issue.getComponentObjects().isEmpty()) {
                List<String> components = new ArrayList<String>();
                for (ProjectComponent pc : issue.getComponentObjects())
                    components.add(pc.getName());
                issueInfo.setComponents(components.toString());
            } else if (extraField.equals(CalendarManager.DUEDATE) && issue.getDueDate() != null)
                issueInfo.setDueDate(dateTimeFormatter.forLoggedInUser().format(issue.getDueDate()));
            else if (extraField.equals(CalendarManager.ENVIRONMENT) && issue.getEnvironment() != null)
                issueInfo.setEnvirounment(issue.getEnvironment());
            else if (extraField.equals(CalendarManager.PRIORITY) && issue.getPriorityObject() != null) {
                issueInfo.setPriority(issue.getPriorityObject().getName());
                issueInfo.setPriorityIconUrl(issue.getPriorityObject().getIconUrl());
            } else if (extraField.equals(CalendarManager.RESOLUTION) && issue.getResolutionObject() != null)
                issueInfo.setResolution(issue.getResolutionObject().getName());
            else if (extraField.equals(CalendarManager.AFFECT) && issue.getAffectedVersions() != null && !issue.getAffectedVersions().isEmpty()) {
                List<String> affectVersions = new ArrayList<String>();
                for (Version ver : issue.getAffectedVersions())
                    affectVersions.add(ver.getName());
                issueInfo.setAffect(affectVersions.toString());
            } else if (extraField.equals(CalendarManager.CREATED))
                issueInfo.setCreated(dateTimeFormatter.forLoggedInUser().format(issue.getCreated()));
            else if (extraField.equals(CalendarManager.UPDATED))
                issueInfo.setUpdated(dateTimeFormatter.forLoggedInUser().format(issue.getUpdated()));
        }
    }

    @PUT
    @Path("{calendarId}/event/{eventId}/")
    public Response moveEvent(@PathParam("calendarId") final int calendarId, @PathParam("eventId") final String eventId, @QueryParam("dayDelta") final int dayDelta, @QueryParam("millisDelta") final int millisDelta, @QueryParam("isDrag") final boolean isDrag) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                ApplicationUser user = getLoggedInApplicationUser();
                IssueService.IssueResult issueResult = issueService.getIssue(ApplicationUsers.toDirectoryUser(user), eventId);
                MutableIssue issue = issueResult.getIssue();

                if (!issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)))
                    throw new IllegalArgumentException("Can not edit issue with key => " + eventId);

                Calendar calendar = calendarManager.getCalendar(calendarId);

                if (isDrag)
                    dragEvent(user, calendar, issue, dayDelta, millisDelta);
                else
                    resizeEvent(user, calendar, issue, dayDelta, millisDelta);
                return null;
            }
        }.getResponse();
    }

    private void dragEvent(ApplicationUser user, Calendar calendar, Issue issue, int dayDelta, int millisDelta) throws Exception {
        if (isDateFieldsNotDraggable(calendar.getEventStart(), calendar.getEventEnd()))
            throw new IllegalArgumentException(String.format("Can not drag event with key => %s, because it contains not draggable event date field", issue.getKey()));

        CustomField eventStartCF = null;
        Timestamp eventStartCFValue = null;
        boolean eventStartIsDueDate = false;
        if (calendar.getEventStart().startsWith("customfield_")) {
            eventStartCF = customFieldManager.getCustomFieldObject(calendar.getEventStart());
            if (eventStartCF == null)
                throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
            eventStartCFValue = (Timestamp) issue.getCustomFieldValue(eventStartCF);
        } else
            eventStartIsDueDate = true;

        CustomField eventEndCF = null;
        Timestamp eventEndCFValue = null;
        boolean eventEndIsDueDate = false;
        if (calendar.getEventEnd() != null) {
            if (calendar.getEventEnd().startsWith("customfield_")) {
                eventEndCF = customFieldManager.getCustomFieldObject(calendar.getEventEnd());
                if (eventEndCF == null)
                    throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
                eventEndCFValue = (Timestamp) issue.getCustomFieldValue(eventEndCF);
            } else
                eventEndIsDueDate = true;
        }

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        String dateFormat = getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT);
        String dateTimeFormat = getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT);

        if (eventStartIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, getLocale()).format(newDueDate));
        } else if (eventStartCFValue != null) {
            String keyForDateFormat = eventStartCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            Timestamp value = getNewTimestamp(eventStartCFValue, dayDelta, millisDelta);
            issueInputParams.addCustomFieldValue(eventStartCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, getLocale()).format(value));
        }

        if (eventEndIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, getLocale()).format(newDueDate));
        } else if (eventEndCF != null && eventEndCFValue != null) {
            String keyForDateFormat = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            Timestamp value = getNewTimestamp(eventEndCFValue, dayDelta, millisDelta);
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, getLocale()).format(value));
        }

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(ApplicationUsers.toDirectoryUser(user), issue.getId(), issueInputParams);
        if (!updateValidationResult.isValid())
            throw new Exception(formatErrorCollection(updateValidationResult.getErrorCollection()));

        IssueService.IssueResult updateResult = issueService.update(ApplicationUsers.toDirectoryUser(user), updateValidationResult);
        if (!updateResult.isValid())
            throw new Exception(formatErrorCollection(updateResult.getErrorCollection()));
    }

    private void resizeEvent(ApplicationUser user, Calendar calendar, Issue issue, int dayDelta, int millisDelta) throws Exception {
        if (isDateFieldNotDraggable(calendar.getEventEnd()))
            throw new IllegalArgumentException(String.format("Can not resize event with key => %s, because it contains not draggable end field", issue.getKey()));

        CustomField eventStartCF;
        Date eventStartDateValue;
        if (calendar.getEventStart().startsWith("customfield_")) {
            eventStartCF = customFieldManager.getCustomFieldObject(calendar.getEventStart());
            if (eventStartCF == null)
                throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
            eventStartDateValue = (Timestamp) issue.getCustomFieldValue(eventStartCF);
        } else {
            eventStartDateValue = retrieveDateByField(issue, calendar.getEventStart());
        }

        if (eventStartDateValue == null)
            throw new IllegalArgumentException("Can not resize event with empty start date field. Issue => " + issue.getKey());


        CustomField eventEndCF = null;
        Date eventEndDateValue = null;
        boolean eventEndIsDueDate = false;
        if (calendar.getEventEnd() != null) {
            if (calendar.getEventEnd().startsWith("customfield_")) {
                eventEndCF = customFieldManager.getCustomFieldObject(calendar.getEventEnd());
                if (eventEndCF == null)
                    throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
                eventEndDateValue = (Timestamp) issue.getCustomFieldValue(eventEndCF);
            } else {
                eventEndIsDueDate = true;
                eventEndDateValue = retrieveDateByField(issue, calendar.getEventEnd());
            }
        }

        if (eventEndDateValue == null)
            throw new IllegalArgumentException("Can not resize event with empty end date field. Issue => " + issue.getKey());

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        String dateFormat = getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT);
        String dateTimeFormat = getApplicationProperties().getDefaultBackedString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT);

        if (eventEndIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, getLocale()).format(newDueDate));
        } else {
            String keyForDateFormat = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            Timestamp value = getNewTimestamp(eventEndDateValue, dayDelta, millisDelta);
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, getLocale()).format(value));
        }

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(ApplicationUsers.toDirectoryUser(user), issue.getId(), issueInputParams);
        if (!updateValidationResult.isValid())
            throw new Exception(formatErrorCollection(updateValidationResult.getErrorCollection()));

        IssueService.IssueResult updateResult = issueService.update(ApplicationUsers.toDirectoryUser(user), updateValidationResult);
        if (!updateResult.isValid())
            throw new Exception(formatErrorCollection(updateResult.getErrorCollection()));
    }

    private String formatErrorCollection(ErrorCollection errorCollection) {
        Collection<String> lines = new ArrayList<String>();
        if (errorCollection.getErrorMessages() != null)
            lines.addAll(errorCollection.getErrorMessages());
        if (errorCollection.getErrors() != null)
            lines.addAll(errorCollection.getErrors().values());
        return StringUtils.join(lines, ", ");
    }

    private boolean isDateFieldsNotDraggable(String startField, String endField) {
        return isDateFieldNotDraggable(startField) && isDateFieldNotDraggable(endField);
    }

    private boolean isDateFieldNotDraggable(String field) {
        return !isDateFieldDraggable(field);
    }

    private boolean isDateFieldDraggable(String field) {
        return !CREATED_DATE_KEY.equals(field) && !UPDATED_DATE_KEY.equals(field) && !RESOLVED_DATE_KEY.equals(field);
    }

    private boolean isDateFieldsDraggable(String startField, String endField) {
        return (!startField.equals(CREATED_DATE_KEY) && !startField.equals(UPDATED_DATE_KEY) && !startField.equals(RESOLVED_DATE_KEY))
                && (endField == null || (!endField.equals(CREATED_DATE_KEY) && !endField.equals(UPDATED_DATE_KEY) && !endField.equals(RESOLVED_DATE_KEY)));
    }

    private Timestamp getNewTimestamp(Date source, int dayDelta, int millisDelta) {
        int summaryMillis = MILLIS_IN_DAY * dayDelta + millisDelta;
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(source);
        gregorianCalendar.add(java.util.Calendar.MILLISECOND, summaryMillis);
        return new Timestamp(gregorianCalendar.getTimeInMillis());
    }

    @PUT
    @Path("/{calendarId}/visibility")
    public Response invertVisibility(@PathParam("calendarId") final int calendarId) {
        return new RestExecutor<Boolean>() {
            @Override
            protected Boolean doAction() throws Exception {
                return calendarManager.invertCalendarVisibility(calendarId);
            }
        }.getResponse();
    }

    private List<Event> getEvents(Calendar calendar,
                                  JqlClauseBuilder jqlBuilder,
                                  String startField,
                                  String endField,
                                  Date startTime,
                                  Date endTime) throws SearchException {
        List<Event> result = new ArrayList<Event>();
        SimpleDateFormat clientDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        CustomField startCF = null;
        if (startField.startsWith("customfield_")) {
            startCF = customFieldManager.getCustomFieldObject(startField);
            if (startCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + startField);
        }

        CustomField endCF = null;
        if (StringUtils.isNotEmpty(endField) && endField.startsWith("customfield_")) {
            endCF = customFieldManager.getCustomFieldObject(endField);
            if (endCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + endField);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        jqlBuilder.and().sub();
        addDateCondition(startField, startTime, endTime, jqlBuilder, simpleDateFormat, false);
        if (StringUtils.isNotEmpty(endField)) {
            jqlBuilder.or();
            addDateCondition(endField, startTime, endTime, jqlBuilder, simpleDateFormat, true);
        }
        jqlBuilder.endsub();
        boolean dateFieldsIsDraggable = isDateFieldsDraggable(startField, endField);

        @SuppressWarnings("deprecation")
        List<Issue> issues = searchService.search(getLoggedInUser(), jqlBuilder.buildQuery(), PagerFilter.getUnlimitedFilter()).getIssues();
        for (Issue issue: issues) {
            try {
                Date startDate = startCF == null ? retrieveDateByField(issue, startField) : retrieveDateByField(issue, startCF);
                Date endDate = null;
                if (StringUtils.isNotEmpty(endField))
                    endDate = endCF == null ? retrieveDateByField(issue, endField) : retrieveDateByField(issue, endCF);

                boolean isAllDay = isAllDayEvent(startCF, endCF, startField, endField);

                Event event = new Event();
                event.setCalendarId(calendar.getID());
                event.setId(issue.getKey());
                event.setTitle(issue.getSummary());
                event.setColor(calendar.getColor());
                event.setAllDay(isAllDay);

                if (startDate == null && endDate == null) { // Something unbelievable
                    log.error("Event " + issue.getKey() + " doesn't contain startDate and endDate");
                    continue;
                }

                if (startDate != null) {
                    event.setStart(clientDateFormat.format(startDate));
                    if (endDate != null)
                        event.setEnd(isAllDay ? clientDateFormat.format(new Date(endDate.getTime() + MILLIS_IN_DAY)) : clientDateFormat.format(endDate));
                } else {
                    event.setStart(clientDateFormat.format(endDate));
                }

                event.setStartEditable(dateFieldsIsDraggable && issueService.isEditable(issue, getLoggedInUser()));
                event.setDurationEditable(isDateFieldDraggable(endField) && startDate != null && endDate != null && issueService.isEditable(issue, getLoggedInUser()));

                result.add(event);
            } catch (Exception e) {
                log.error(String.format("Error while trying to translate issue => %s to event", issue.getKey()), e);
            }
        }
        return result;
    }

    private void addDateCondition(String field, Date startTime, Date endTime, JqlClauseBuilder jcb, SimpleDateFormat simpleDateFormat, boolean nullable) {
        if (field.equals(DUE_DATE_KEY))
            jcb.dueBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(CREATED_DATE_KEY))
            jcb.createdBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(UPDATED_DATE_KEY))
            jcb.updatedBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(RESOLVED_DATE_KEY))
            jcb.updatedBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.startsWith("customfield_")){
            CustomField customField = customFieldManager.getCustomFieldObject(field);
            if (customField == null)
                throw new IllegalArgumentException("Bad custom field id => " + field);
            jcb.addDateRangeCondition("cf[" + customField.getIdAsLong() + "]", startTime, endTime);
        } else
            throw new IllegalArgumentException("Bad field => " + field);
    }

    private List<Event> getProjectEvents(Calendar calendar,
                                  long projectId,
                                  String startField,
                                  String endField,
                                  Date startTime,
                                  Date endTime) throws SearchException {

        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder();
        jqlBuilder.project(projectId);
        return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime);
    }

    private List<Event> getFilterEvents(Calendar calendar,
                                         long filterId,
                                         String startField,
                                         String endField,
                                         Date startTime,
                                         Date endTime) throws SearchException {
        JiraServiceContext jsCtx = new JiraServiceContextImpl(getLoggedInApplicationUser());
        SearchRequest filter = searchRequestService.getFilter(jsCtx, filterId);

        if (filter == null) {
            log.error("Filter with id => " + filterId + " is null. Maybe it was deleted");
            return new ArrayList<Event>(0);
        }

        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder(filter.getQuery());
        return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime);
    }

    private boolean isAllDayEvent(@Nullable CustomField startCF, @Nullable CustomField endCF,
                                  @Nullable String startField, @Nullable String endField) {
        boolean startFieldForDate;
        if (startCF != null)
            startFieldForDate = isDateField(startCF);
        else if (startField != null)
            startFieldForDate = startField.equals(DUE_DATE_KEY);
        else
            throw new IllegalArgumentException("Event start can not be null");

        boolean endFieldForDate;
        if (endCF != null) {
            endFieldForDate = isDateField(endCF);
        } else if (endField != null) {
            endFieldForDate = endField.equals(DUE_DATE_KEY);
        } else {
            return startFieldForDate;
        }

        return endFieldForDate && startFieldForDate;
    }

    private boolean isDateTimeField(CustomField cf) {
        return cf.getCustomFieldType() instanceof DateTimeCFType;
    }

    private boolean isDateField(CustomField cf) {
        return !isDateTimeField(cf);
    }

    private Date retrieveDateByField(Issue issue, String field) {
        if (field.equals(DUE_DATE_KEY))
            return issue.getDueDate();
        if (field.equals(CREATED_DATE_KEY))
            return issue.getCreated();
        if (field.equals(UPDATED_DATE_KEY))
            return issue.getUpdated();
        if (field.equals(RESOLVED_DATE_KEY))
            return issue.getResolutionDate();
        throw new IllegalArgumentException("Unknown field => " + field);
    }

    private Date retrieveDateByField(Issue issue, CustomField customField) {
        if (!(customField.getCustomFieldType() instanceof com.atlassian.jira.issue.fields.DateField))
            throw new IllegalArgumentException("Bad date time => " + customField.getName());
        return (Date) issue.getCustomFieldValue(customField);
    }

    @SuppressWarnings("unused")
    public boolean isHideWeekends() {
        UserData userData = calendarManager.getUserData();
        return userData != null && userData.isHideWeekends();
    }

    public String getUserAvatarSrc(String userKey) {
        return avatarService.getAvatarURL(getLoggedInApplicationUser(), getUserManager().getUserByKey(userKey), Avatar.Size.SMALL).toString();
    }

    @SuppressWarnings("unused")
    public List<String> getColors() {
        return Arrays.asList("#5dab3e", "#d7ad43", "#3e6894", "#c9dad8", "#588e87", "#e18434",
                "#83382A", "#D04A32", "#3C2B28", "#87A4C0", "#A89B95");
    }

    @SuppressWarnings("unused")
    public List<DateField> getDateFields() {
        List<DateField> dateFields = new ArrayList<DateField>();
        dateFields.add(DateField.of(CREATED_DATE_KEY, i18nHelper.getText("issue.field.created")));
        dateFields.add(DateField.of(UPDATED_DATE_KEY, i18nHelper.getText("issue.field.updated")));
        dateFields.add(DateField.of(RESOLVED_DATE_KEY, i18nHelper.getText("common.concepts.resolved")));
        dateFields.add(DateField.of(DUE_DATE_KEY, i18nHelper.getText("issue.field.duedate")));

        for (CustomField customField : customFieldManager.getCustomFieldObjects())
            if (customField.getCustomFieldType() instanceof com.atlassian.jira.issue.fields.DateField)
                dateFields.add(DateField.of(customField.getId(), customField.getName()));

        return dateFields;
    }

    @GET
    @Path("/displayedFields")
    public Map<String,String> getDisplayedFields() {
        Map<String,String> result = new LinkedHashMap<String, String>(CalendarManager.DISPLAYED_FIELDS.size());
        for (String field: CalendarManager.DISPLAYED_FIELDS)
            result.put(field, i18nHelper.getText(field));
        return result;
    }

    @GET
    @Path("/eventSources")
    public Response getEventSources(@QueryParam("filter") final String filter) {
        return new RestExecutor<AllSources>() {
            @Override
            protected AllSources doAction() throws Exception {
                return new AllSources(getProjectSources(filter), getFilterSources(filter));
            }
        }.getResponse();
    }

    private List<SourceField> getProjectSources(String filter) {
        List<SourceField> result = new ArrayList<SourceField>();
        filter = filter.trim().toLowerCase();
        int length = 0;
        List<Project> allProjects = projectService.getAllProjects(getLoggedInApplicationUser()).get();
        for (Project project: allProjects) {
            if (project.getName().toLowerCase().contains(filter) || project.getKey().toLowerCase().contains(filter)) {
                result.add(new SourceField("project_" + project.getId(), formatProject(project), project.getAvatar().getId()));
                length++;
                if (length == 10)
                    break;
            }
        }

        Collections.sort(result, new Comparator<SourceField>() {
            @Override
            public int compare(SourceField fProject, SourceField sProject) {
                return fProject.getText().toLowerCase().compareTo(sProject.getText().toLowerCase());
            }
        });

        return result;
    }

    private String formatProject(Project project) {
        return String.format("%s (%s)", project.getName(), project.getKey());
    }

    private List<SourceField> getFilterSources(String filter) {
        List<SourceField> result = new ArrayList<SourceField>();
        SharedEntitySearchParametersBuilder builder = new SharedEntitySearchParametersBuilder();
        builder.setName(StringUtils.isBlank(filter) ? null : filter);
        builder.setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD);
        builder.setSortColumn(SharedEntityColumn.NAME, true);
        builder.setEntitySearchContext(SharedEntitySearchContext.USE);

        SharedEntitySearchResult<SearchRequest> searchResults
                = searchRequestService.search(new JiraServiceContextImpl(getLoggedInApplicationUser()), builder.toSearchParameters(), 0, 10);

        for (SearchRequest search: searchResults.getResults())
            result.add(new SourceField("filter_" + search.getId(), search.getName(), 0));

        return result;
    }

    private boolean isAdministrator() {
        return isAdministrator(getLoggedInApplicationUser());
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(Permissions.ADMINISTER, user);
    }
}
