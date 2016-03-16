package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
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
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.model.SubjectType;
import ru.mail.jira.plugins.calendar.rest.dto.DateField;
import ru.mail.jira.plugins.calendar.rest.dto.IssueSourceDto;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionItemDto;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionSubjectDto;
import ru.mail.jira.plugins.calendar.rest.dto.SelectItemDto;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarServiceImpl;
import ru.mail.jira.plugins.calendar.service.PermissionUtils;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/calendar/config")
@Produces(MediaType.APPLICATION_JSON)
public class RestConfigurationService {
    private final AvatarService avatarService;
    private final CustomFieldManager customFieldManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final I18nHelper i18nHelper;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectManager projectManager;
    private final ProjectService projectService;
    private final ProjectRoleManager projectRoleManager;
    private final SearchRequestService searchRequestService;
    private final UserManager userManager;

    public RestConfigurationService(AvatarService avatarService, CustomFieldManager customFieldManager,
                                    GlobalPermissionManager globalPermissionManager, GroupManager groupManager, I18nHelper i18nHelper,
                                    JiraAuthenticationContext jiraAuthenticationContext,
                                    ProjectManager projectManager, ProjectService projectService,
                                    ProjectRoleManager projectRoleManager, SearchRequestService searchRequestService, UserManager userManager) {
        this.avatarService = avatarService;
        this.customFieldManager = customFieldManager;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.i18nHelper = i18nHelper;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectManager = projectManager;
        this.projectService = projectService;
        this.projectRoleManager = projectRoleManager;
        this.searchRequestService = searchRequestService;
        this.userManager = userManager;
    }

    @GET
    @Path("/displayedFields")
    public Map<String, String> getDisplayedFields() {
        Map<String, String> result = new LinkedHashMap<String, String>(CalendarServiceImpl.DISPLAYED_FIELDS.size());
        for (String field : CalendarServiceImpl.DISPLAYED_FIELDS)
            result.put(field, i18nHelper.getText(field));
        for (CustomField customField : customFieldManager.getCustomFieldObjects())
            result.put(customField.getId(), customField.getName());
        return result;
    }

    @GET
    @Path("/dateFields")
    public List<DateField> getDateFields() {
        List<DateField> dateFields = new ArrayList<DateField>();
        dateFields.add(DateField.of(CalendarEventService.CREATED_DATE_KEY, i18nHelper.getText("issue.field.created")));
        dateFields.add(DateField.of(CalendarEventService.UPDATED_DATE_KEY, i18nHelper.getText("issue.field.updated")));
        dateFields.add(DateField.of(CalendarEventService.RESOLVED_DATE_KEY, i18nHelper.getText("common.concepts.resolved")));
        dateFields.add(DateField.of(CalendarEventService.DUE_DATE_KEY, i18nHelper.getText("issue.field.duedate")));

        for (CustomField customField : customFieldManager.getCustomFieldObjects())
            if (customField.getCustomFieldType() instanceof com.atlassian.jira.issue.fields.DateField)
                dateFields.add(DateField.of(customField.getId(), customField.getName()));

        return dateFields;
    }

    @GET
    @Path("/eventSources")
    public Response getEventSources(@QueryParam("filter") final String filter) {
        return new RestExecutor<IssueSourceDto>() {
            @Override
            protected IssueSourceDto doAction() throws Exception {
                return new IssueSourceDto(getProjectSources(filter), getFilterSources(filter));
            }
        }.getResponse();
    }

    @GET
    @Path("permission/subjects")
    public Response getPermissionSubjects(@QueryParam("filter") final String filter) {
        return new RestExecutor<PermissionSubjectDto>() {
            @Override
            protected PermissionSubjectDto doAction() throws Exception {
                return new PermissionSubjectDto(getUsers(filter), getGroups(jiraAuthenticationContext.getUser(), filter), getProjectRoles(jiraAuthenticationContext.getUser(), filter));
            }
        }.getResponse();
    }

    private List<PermissionItemDto> getProjectRoles(ApplicationUser user, String filter) {
        List<PermissionItemDto> result = new ArrayList<PermissionItemDto>();
        filter = filter.trim().toLowerCase();

        List<Project> allProjects = isAdministrator(user) ? projectManager.getProjectObjects() : projectService.getAllProjects(user).get();
        for (Project project : allProjects) {
            Collection<ProjectRole> projectRoles = projectRoleManager.getProjectRoles(user, project);
            for (ProjectRole role : projectRoles)
                if (StringUtils.containsIgnoreCase(role.getName(), filter)
                        || StringUtils.containsIgnoreCase(project.getName(), filter)
                        || StringUtils.containsIgnoreCase(project.getKey(), filter))
                    result.add(new PermissionItemDto(PermissionUtils.projectRoleSubject(project.getId(), role.getId()),
                                                     PermissionUtils.projectRoleSubject(project.getName(), role.getName()),
                                                     SubjectType.PROJECT_ROLE.name(),
                                                     null,
                                                     String.format("projectavatar?pid=%d&avatarId=%d&size=xxmall", project.getId(), project.getAvatar().getId())));
            if (result.size() >= 10)
                break;
        }
        return result;
    }

    private List<PermissionItemDto> getGroups(ApplicationUser user, String filter) {
        filter = filter.trim().toLowerCase();

        Collection<Group> groups = isAdministrator(user) ? groupManager.getAllGroups() : groupManager.getGroupsForUser(user.getName());
        List<PermissionItemDto> result = new ArrayList<PermissionItemDto>();
        for (Group group : groups) {
            if (StringUtils.containsIgnoreCase(group.getName(), filter))
                result.add(new PermissionItemDto(group.getName(), group.getName(), SubjectType.GROUP.name(), null, ""));
            if (result.size() == 10)
                break;
        }
        return result;
    }

    private List<PermissionItemDto> getUsers(String filter) {
        List<PermissionItemDto> result = new ArrayList<PermissionItemDto>();
        filter = filter.trim().toLowerCase();

        for (ApplicationUser user : userManager.getAllApplicationUsers()) {
            if (StringUtils.containsIgnoreCase(user.getDisplayName(), filter)
                    || StringUtils.containsIgnoreCase(user.getKey(), filter)
                    || StringUtils.containsIgnoreCase(user.getName(), filter))
                result.add(new PermissionItemDto(user.getKey(),
                                                 String.format("%s - %s (%s)", user.getDisplayName(), user.getEmailAddress(), user.getKey()),
                                                 SubjectType.USER.name(),
                                                 null,
                                                 getUserAvatarSrc(user)));
            if (result.size() == 10)
                break;
        }
        return result;
    }

    private List<SelectItemDto> getProjectSources(String filter) {
        List<SelectItemDto> result = new ArrayList<SelectItemDto>();
        filter = filter.trim().toLowerCase();
        int length = 0;
        List<Project> allProjects = projectService.getAllProjects(jiraAuthenticationContext.getUser()).get();
        for (Project project : allProjects) {
            if (project.getName().toLowerCase().contains(filter) || project.getKey().toLowerCase().contains(filter)) {
                result.add(new SelectItemDto("project_" + project.getId(), String.format("%s (%s)", project.getName(), project.getKey()), project.getAvatar().getId()));
                length++;
                if (length == 10)
                    break;
            }
        }

        Collections.sort(result, new Comparator<SelectItemDto>() {
            @Override
            public int compare(SelectItemDto fProject, SelectItemDto sProject) {
                return fProject.getText().toLowerCase().compareTo(sProject.getText().toLowerCase());
            }
        });

        return result;
    }

    private List<SelectItemDto> getFilterSources(String filter) {
        List<SelectItemDto> result = new ArrayList<SelectItemDto>();
        SharedEntitySearchParametersBuilder builder = new SharedEntitySearchParametersBuilder();
        builder.setName(StringUtils.isBlank(filter) ? null : filter);
        builder.setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD);
        builder.setSortColumn(SharedEntityColumn.NAME, true);
        builder.setEntitySearchContext(SharedEntitySearchContext.USE);

        SharedEntitySearchResult<SearchRequest> searchResults = searchRequestService.search(new JiraServiceContextImpl(jiraAuthenticationContext.getUser()), builder.toSearchParameters(), 0, 10);

        for (SearchRequest search : searchResults.getResults())
            result.add(new SelectItemDto("filter_" + search.getId(), search.getName(), 0));

        return result;
    }

    private String getUserAvatarSrc(ApplicationUser user) {
        return avatarService.getAvatarURL(user, userManager.getUserByKey(user.getKey()), Avatar.Size.SMALL).toString();
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
