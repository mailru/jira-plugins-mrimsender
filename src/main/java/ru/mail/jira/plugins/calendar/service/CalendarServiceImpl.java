package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.exception.UpdateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableSet;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.*;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.FavouriteQuickFilter;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.PermissionType;
import ru.mail.jira.plugins.calendar.model.QuickFilter;
import ru.mail.jira.plugins.calendar.model.UserCalendar;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarDto;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarSettingDto;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionItemDto;
import ru.mail.jira.plugins.calendar.rest.dto.QuickFilterDto;
import ru.mail.jira.plugins.commons.RestFieldException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@ExportAsService(CalendarService.class)
public class CalendarServiceImpl implements CalendarService {
    private final static Logger log = LoggerFactory.getLogger(CalendarServiceImpl.class);

    private final static Pattern COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    private static final Set<String> SUPPORTED_GROUPS = ImmutableSet.of(
        "component",
        "fixVersion",
        "affectsVersion",
        "labels",
        "assignee",
        "reporter",
        "issueType",
        "project",
        "priority",
        "epicLink"
    );

    public static final String DESCRIPTION = "common.words.description";
    public static final String STATUS = "common.words.status";
    public static final String LABELS = "common.concepts.labels";
    public static final String COMPONENTS = "common.concepts.components";
    public static final String DUEDATE = "issue.field.duedate";
    public static final String ENVIRONMENT = "common.words.env";
    public static final String PRIORITY = "issue.field.priority";
    public static final String RESOLUTION = "issue.field.resolution";
    public static final String AFFECT = "issue.field.version";
    public static final String CREATED = "issue.field.created";
    public static final String UPDATED = "issue.field.updated";
    public static final String REPORTER = "issue.field.reporter";
    public static final String ASSIGNEE = "issue.field.assignee";
    public static final List<String> DISPLAYED_FIELDS = new ArrayList<String>() {{
        add(DESCRIPTION);
        add(STATUS);
        add(ASSIGNEE);
        add(REPORTER);
        add(PRIORITY);
        add(CREATED);
        add(UPDATED);
        add(DUEDATE);
        add(COMPONENTS);
        add(ENVIRONMENT);
        add(LABELS);
        add(RESOLUTION);
        add(AFFECT);
    }};

    private final ActiveObjects ao;
    private final CustomFieldManager customFieldManager;
    private final I18nResolver i18nResolver;
    private final GroupManager groupManager;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final PermissionManager permissionManager;
    private final PermissionService permissionService;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final QuickFilterService quickFilterService;
    private final SearchRequestService searchRequestService;
    private final SearchRequestManager searchRequestManager;
    private final UserCalendarService userCalendarService;
    private final UserManager userManager;

    @Autowired
    public CalendarServiceImpl(
        @ComponentImport CustomFieldManager customFieldManager,
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport GroupManager groupManager,
        @ComponentImport PermissionManager permissionManager,
        @ComponentImport ProjectManager projectManager,
        @ComponentImport ProjectRoleManager projectRoleManager,
        @ComponentImport SearchRequestService searchRequestService,
        @ComponentImport SearchRequestManager searchRequestManager,
        @ComponentImport UserManager userManager,
        @ComponentImport ActiveObjects ao,
        JiraDeprecatedService jiraDeprecatedService,
        PermissionService permissionService,
        QuickFilterService quickFilterService,
        UserCalendarService userCalendarService
    ) {
        this.ao = ao;
        this.customFieldManager = customFieldManager;
        this.i18nResolver = i18nResolver;
        this.groupManager = groupManager;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.permissionManager = permissionManager;
        this.permissionService = permissionService;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.quickFilterService = quickFilterService;
        this.searchRequestService = searchRequestService;
        this.searchRequestManager = searchRequestManager;
        this.userCalendarService = userCalendarService;
        this.userManager = userManager;
    }


    public Calendar getCalendar(final int id) throws GetException {
        Calendar calendar = ao.get(Calendar.class, id);
        if (calendar == null)
            throw new GetException("No Calendar with id=" + id);
        return calendar;
    }

    public CalendarSettingDto getCalendarSettingDto(ApplicationUser user, int id) throws GetException {
        CalendarSettingDto result = new CalendarSettingDto();
        Calendar calendar = getCalendar(id);
        result.setSelectedName(calendar.getName());
        result.setSelectedColor(calendar.getColor());
        result.setSelectedEventStartId(calendar.getEventStart());
        result.setSelectedEventEndId(calendar.getEventEnd());

        if (StringUtils.isNotEmpty(calendar.getDisplayedFields()))
            result.setSelectedDisplayedFields(Arrays.asList(calendar.getDisplayedFields().split(",")));
        result.setShowIssueStatus(calendar.isShowIssueStatus());
        result.setGanttEnabled(calendar.isGanttEnabled());

        fillSelectedSourceFields(user, result, calendar);

        result.setCanAdmin(permissionService.hasAdminPermission(user, calendar));

        List<PermissionItemDto> permissions = new ArrayList<PermissionItemDto>();
        for (Permission permission : calendar.getPermissions()) {
            PermissionType permissionType = permission.getPermissionType();
            PermissionItemDto itemDto = null;
            switch (permissionType) {
                case USER:
                    ApplicationUser subjectUser = userManager.getUserByKey(permission.getPermissionValue());
                    if (subjectUser != null)
                        itemDto = PermissionItemDto.buildUserDto(subjectUser.getKey(), subjectUser.getDisplayName(), subjectUser.getEmailAddress(), subjectUser.getName(),
                                                                 PermissionUtils.getAccessType(permission.isAdmin(), true),
                                                                 permissionService.getPermissionAvatar(permission, permissionType));
                    break;
                case GROUP:
                    Group group = groupManager.getGroup(permission.getPermissionValue());
                    if (group != null)
                        itemDto = PermissionItemDto.buildGroupDto(permission.getPermissionValue(), group.getName(),
                                                                  PermissionUtils.getAccessType(permission.isAdmin(), true));
                    break;
                case PROJECT_ROLE:
                    Long projectId = PermissionUtils.getProject(permission.getPermissionValue());
                    Long projectRoleId = PermissionUtils.getProjectRole(permission.getPermissionValue());
                    if (projectId == null || projectRoleId == null)
                        break;
                    Project project = projectManager.getProjectObj(projectId);
                    ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleId);
                    String projectName = null;
                    String projectRoleName = projectRole != null ? projectRole.getName() : null;
                    if (project != null && permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false))
                        projectName = project.getName();
                    itemDto = PermissionItemDto.buildProjectRoleDto(permission.getPermissionValue(), projectName, projectRoleName,
                                                                    PermissionUtils.getAccessType(permission.isAdmin(), true),
                                                                    permissionService.getPermissionAvatar(permission, PermissionType.PROJECT_ROLE));
                    break;
            }
            if (itemDto != null)
                permissions.add(itemDto);
        }
        if (permissions.size() > 0)
            result.setPermissions(permissions);
        return result;
    }

    public CalendarDto[] getAllCalendars(final ApplicationUser user) {
        return fillUserCalendarDtos(user, ao.find(Calendar.class));
    }

    @Override
    public CalendarDto[] getUserCalendars(ApplicationUser user) {
        return fillUserCalendarDtos(user);
    }

    @Override
    public CalendarDto getUserCalendar(ApplicationUser user, int id) throws GetException {
        CalendarDto result = null;
        UserCalendar userCalendar = userCalendarService.get(id, user.getKey());
        try {
            Calendar calendar = getCalendar(userCalendar.getCalendarId());
            boolean canAdmin = permissionService.hasAdminPermission(user, calendar);
            boolean canUse = canAdmin || permissionService.hasUsePermission(user, calendar);
            if (canAdmin || canUse)
                result = buildCalendarOutput(user, userCalendar, calendar, true, canAdmin, userCalendar.isEnabled(), true, userCalendarService.getUsersCount(calendar.getID()));
        } catch (GetException e) {
            //ignore
        }
        if (result == null) {
            result = buildCalendarOutput(user, userCalendar, null, false, false, false, true, 0);
            result.setHasError(true);
            result.setError(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailable"));
        }
        return result;
    }

    @Override
    public CalendarDto[] findCalendars(ApplicationUser user, Integer[] calendarIds) {
        return fillUserCalendarDtos(user, ao.get(Calendar.class, calendarIds));
    }

    public CalendarDto createCalendar(final ApplicationUser user, final CalendarSettingDto calendarSettingDto) throws GetException {
        validateCalendar(user, calendarSettingDto, true);
        Calendar calendar = ao.create(Calendar.class);
        calendar.setAuthorKey(user.getKey());
        setCalendarFields(calendar, calendarSettingDto);

        permissionService.updatePermissions(calendar, calendarSettingDto.getPermissions());
        userCalendarService.addCalendarToUser(user.getKey(), calendar, true);
        CalendarDto result = new CalendarDto(null, calendar);
        result.setFavorite(true);
        result.setVisible(true);

        //update OneToMany entities after saving
        calendar = getCalendar(calendar.getID());
        boolean canAdmin = permissionService.hasAdminPermission(user, calendar);
        boolean canUse = canAdmin || permissionService.hasUsePermission(user, calendar);
        result.setChangable(canAdmin);
        result.setViewable(canUse);
        result.setEventsEditable(canAdmin);
        if (!canAdmin && !canUse) {
            result.setHasError(true);
            result.setError(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailable"));
        }
        return result;
    }

    public CalendarDto updateCalendar(final ApplicationUser user, final CalendarSettingDto calendarSettingDto) throws GetException {
        Calendar calendar = getCalendar(calendarSettingDto.getId());
        if (!permissionService.hasAdminPermission(user, calendar))
            throw new SecurityException("No permission to edit calendar");

        validateCalendar(user, calendarSettingDto, false);
        setCalendarFields(calendar, calendarSettingDto);
        permissionService.updatePermissions(calendar, calendarSettingDto.getPermissions());

        //update OneToMany entities after saving
        calendar = getCalendar(calendarSettingDto.getId());
        UserCalendar userCalendar = userCalendarService.find(calendar.getID(), user.getKey());
        boolean canAdmin = permissionService.hasAdminPermission(user, calendar);
        boolean canUse = canAdmin || permissionService.hasUsePermission(user, calendar);
        return buildCalendarOutput(user, userCalendar, calendar, canUse, canAdmin, userCalendar != null && userCalendar.isEnabled(), userCalendar != null, userCalendarService.getUsersCount(calendar.getID()));
    }

    public void deleteCalendar(final ApplicationUser user, final int calendarId) throws GetException {
        Calendar calendar = getCalendar(calendarId);
        if (!permissionService.hasAdminPermission(user, calendar))
            throw new SecurityException("No permission to edit calendar");
        permissionService.removeCalendarPermissions(calendar);
        ao.delete(ao.find(Event.class, Query.select().where("CALENDAR_ID = ?", calendarId)));
        ao.delete(ao.find(EventTypeReminder.class, Query.select().where("CALENDAR_ID = ?", calendarId)));
        ao.delete(ao.find(EventType.class, Query.select().where("CALENDAR_ID = ?", calendarId)));
        userCalendarService.removeCalendar(user.getKey(), calendarId);
        quickFilterService.deleteQuickFilterByCalendarId(calendarId);
        ao.delete(calendar);
    }

    public void updateCalendarVisibility(final int calendarId, final ApplicationUser user, final boolean visible) {
        try {
            userCalendarService.updateCalendarVisibility(calendarId, user.getKey(), visible);
        } catch (GetException e) {
            log.error("Can't get UserCalendar for calendar={} and user={}", calendarId, user.getKey());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addToFavouriteQuickFilter(int calendarId, ApplicationUser user, int id, boolean addToFavourite) throws UpdateException {
        try {
            userCalendarService.addToFavouriteQuickFilter(calendarId, user.getKey(), id, addToFavourite);
        } catch (GetException e) {
            log.error("Can't get UserCalendar for calendar={} and user={}", calendarId, user.getKey());
        }
    }

    @Override
    public void selectQuickFilter(int calendarId, ApplicationUser user, int id, boolean selected) {
        try {
            userCalendarService.selectQuickFilter(calendarId, user.getKey(), id, selected);
        } catch (Exception e) {
            log.error("Can't get UserCalendar for calendar={} and user={}", calendarId, user.getKey());
        }
    }

    private void setCalendarFields(Calendar calendar, CalendarSettingDto calendarSettingDto) {
        calendar.setName(calendarSettingDto.getSelectedName());
        String selectedSourceValue = calendarSettingDto.getSelectedSourceValue();
        if (selectedSourceValue != null) {
            calendar.setSource(String.format("%s_%s", calendarSettingDto.getSelectedSourceType(), selectedSourceValue));
        } else {
            calendar.setSource(calendarSettingDto.getSelectedSourceType());
        }
        calendar.setColor(calendarSettingDto.getSelectedColor());
        calendar.setEventStart(StringUtils.trimToNull(calendarSettingDto.getSelectedEventStartId()));
        calendar.setEventEnd(StringUtils.trimToNull(calendarSettingDto.getSelectedEventEndId()));
        calendar.setDisplayedFields(StringUtils.join(calendarSettingDto.getSelectedDisplayedFields(), ","));
        calendar.setShowIssueStatus(calendarSettingDto.isShowIssueStatus());
        calendar.setGanttEnabled(calendarSettingDto.isGanttEnabled());
        calendar.save();
    }

    private CalendarDto[] fillUserCalendarDtos(final ApplicationUser user) {
        List<CalendarDto> result = new ArrayList<CalendarDto>();
        UserCalendar[] userCalendars = userCalendarService.find(user.getKey());
        for (UserCalendar userCalendar : userCalendars) {
            CalendarDto output = null;
            try {
                Calendar calendar = getCalendar(userCalendar.getCalendarId());
                boolean canAdmin = permissionService.hasAdminPermission(user, calendar);
                boolean canUse = canAdmin || permissionService.hasUsePermission(user, calendar);
                if (canAdmin || canUse)
                    output = buildCalendarOutput(user, userCalendar, calendar, true, canAdmin, userCalendar.isEnabled(), true, userCalendarService.getUsersCount(calendar.getID()));
            } catch (GetException e) {
                //ignore
            }
            if (output == null) {
                output = buildCalendarOutput(user, userCalendar, null, false, false, false, true, 0);
                output.setHasError(true);
                output.setError(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailable"));
            }
            result.add(output);
        }
        return result.toArray(new CalendarDto[result.size()]);
    }

    private CalendarDto[] fillUserCalendarDtos(final ApplicationUser user, Calendar[] calendars) {
        List<CalendarDto> result = new ArrayList<CalendarDto>();
        for (Calendar calendar : calendars) {
            if (calendar == null)
                continue;
            boolean canAdmin = permissionService.hasAdminPermission(user, calendar);
            boolean canUse = canAdmin || permissionService.hasUsePermission(user, calendar);
            UserCalendar userCalendar = userCalendarService.find(calendar.getID(), user.getKey());
            if (canAdmin || canUse || userCalendar != null) {
                CalendarDto output = buildCalendarOutput(user, userCalendar, calendar, canUse, canAdmin, userCalendar != null && userCalendar.isEnabled(), userCalendar != null, userCalendarService.getUsersCount(calendar.getID()));
                result.add(output);
            }
        }
        return result.toArray(new CalendarDto[result.size()]);
    }

    private CalendarDto buildCalendarOutput(ApplicationUser user, UserCalendar userCalendar, Calendar calendar, boolean canUse, boolean changable, boolean visible, boolean favorite, int usersCount) {
        CalendarDto output = new CalendarDto(userCalendar, calendar);
        output.setViewable(canUse);
        output.setChangable(changable);
        output.setVisible(visible);
        output.setEventsEditable(changable);
        output.setFavorite(favorite);
        output.setUsersCount(usersCount);

        if (calendar != null) {
            String filterHasNotAvailableError = checkThatCalendarSourceHasAvailable(user, calendar);
            if (filterHasNotAvailableError != null) {
                output.setHasError(true);
                output.setError(filterHasNotAvailableError);
            }

            List<QuickFilterDto> favouriteQuickFilters = new ArrayList<QuickFilterDto>();
            if (userCalendar != null) {
                for (FavouriteQuickFilter favouriteQuickFilter : userCalendar.getFavouriteQuickFilters()) {
                    QuickFilter quickFilter = favouriteQuickFilter.getQuickFilter();
                    if (quickFilter.isShare() || quickFilter.getCreatorKey().equals(user.getKey()))
                        favouriteQuickFilters.add(new QuickFilterDto(quickFilter.getID(), quickFilter.getName(), favouriteQuickFilter.isSelected()));
                }
            }
            output.setFavouriteQuickFilters(favouriteQuickFilters);
        }
        if (!changable && !canUse) {
            output.setHasError(true);
            output.setError(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailable"));
        }
        return output;
    }

    @Nullable
    private String checkThatCalendarSourceHasAvailable(ApplicationUser user, Calendar calendar) {
        if (calendar.getSource().startsWith("filter_")) {
            JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(user);
            searchRequestService.getFilter(jiraServiceContext, Long.valueOf(calendar.getSource().substring("filter_".length())));
            return jiraServiceContext.getErrorCollection().hasAnyErrors() ? i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailableFilterSource") : null;
        } else if (calendar.getSource().startsWith("project_")) {
            Project project = projectManager.getProjectObj(Long.parseLong(calendar.getSource().substring("project_".length())));
            return project == null || !permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false) ? i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailableProjectSource") : null;
        }
        return null;
    }

    private void fillSelectedSourceFields(ApplicationUser user, CalendarSettingDto dto, Calendar calendar) {
        String source = calendar.getSource();
        if (source.startsWith("project_")) {
            dto.setSelectedSourceType("project");
            long projectId = Long.parseLong(source.substring("project_".length()));
            dto.setSelectedSourceValue(String.valueOf(projectId));
            Project project = projectManager.getProjectObj(projectId);
            if (project == null || !permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false)) {
                dto.setSelectedSourceIsUnavailable(true);
                dto.setSelectedSourceName(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailableProjectSource"));
            } else {
                dto.setSelectedSourceName(String.format("%s (%s)", project.getName(), project.getKey()));
                dto.setSelectedSourceAvatarId(project.getAvatar().getId());
            }
        } else if (source.startsWith("filter_")) {
            dto.setSelectedSourceType("filter");
            long filterId = Long.parseLong(source.substring("filter_".length()));
            dto.setSelectedSourceValue(String.valueOf(filterId));
            SearchRequest filter = searchRequestService.getFilter(new JiraServiceContextImpl(user), filterId);
            if (filter == null) {
                dto.setSelectedSourceIsUnavailable(true);
                dto.setSelectedSourceName(i18nResolver.getText("ru.mail.jira.plugins.calendar.unavailableFilterSource"));
            } else {
                dto.setSelectedSourceName(filter.getName());
            }
        } else if (source.startsWith("jql_")) {
            dto.setSelectedSourceType("jql");
            dto.setSelectedSourceValue(StringUtils.substringAfter(source, "jql_"));
        } else if (source.equals("basic")) {
            dto.setSelectedSourceType("basic");
        } else { // theoretically it isn't possible
            dto.setSelectedSourceName("Unknown source");
        }
    }

    private void validateCalendar(ApplicationUser user, CalendarSettingDto calendarSettingDto, boolean isCreate) {
        if (user == null)
            throw new IllegalArgumentException("User doesn't exist");
        if (StringUtils.isBlank(calendarSettingDto.getSelectedName()))
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("common.words.name")), "name");

        String selectedSourceType = calendarSettingDto.getSelectedSourceType();

        if (!selectedSourceType.equals("basic") && !selectedSourceType.equals("project") && !selectedSourceType.startsWith("filter") && !selectedSourceType.startsWith("jql"))
            throw new IllegalArgumentException("Bad source => " + selectedSourceType);

        if (StringUtils.isBlank(calendarSettingDto.getSelectedSourceValue()) && !selectedSourceType.equals("basic"))
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.dialog.source." + selectedSourceType)), "source");
        try {
            if (selectedSourceType.equals("project")) {
                long projectId = Long.parseLong(calendarSettingDto.getSelectedSourceValue());
                if (isCreate) {
                    Project project = projectManager.getProjectObj(projectId);
                    if (project == null)
                        throw new RestFieldException("Can not find project with id => " + projectId, "source");

                    if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user, false))
                        throw new RestFieldException("No Permission to browse project " + project.getName(), "source");
                }
            } else if (selectedSourceType.equals("filter")) {
                long filterId = Long.parseLong(calendarSettingDto.getSelectedSourceValue());
                if (isCreate) {
                    if (searchRequestManager.getSearchRequestById(filterId) == null)
                        throw new RestFieldException("Can not find filter with id " + filterId, "source");

                    JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
                    searchRequestService.getFilter(serviceContext, filterId);
                    if (serviceContext.getErrorCollection().hasAnyErrors())
                        throw new RestFieldException(serviceContext.getErrorCollection().getErrorMessages().toString(), "source");
                }
            } else if (selectedSourceType.equals("jql")) {
                SearchService.ParseResult parseResult = jiraDeprecatedService.searchService.parseQuery(user, calendarSettingDto.getSelectedSourceValue());
                if (!parseResult.isValid())
                    throw new RestFieldException(StringUtils.join(parseResult.getErrors().getErrorMessages(), "\n"), "source");
                MessageSet validateMessages = jiraDeprecatedService.searchService.validateQuery(user, parseResult.getQuery());
                if (validateMessages.hasAnyErrors())
                    throw new RestFieldException(StringUtils.join(validateMessages.getErrorMessages(), "\n"), "source");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad source => " + calendarSettingDto.getSelectedSourceValue());
        }

        if (StringUtils.isBlank(calendarSettingDto.getSelectedColor()))
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("admin.common.words.color")), "color");
        if (!COLOR_PATTERN.matcher(calendarSettingDto.getSelectedColor()).matches())
            throw new IllegalArgumentException("Bad color => " + calendarSettingDto.getSelectedColor());

        if (!selectedSourceType.equals("basic")) {
            if (StringUtils.isBlank(calendarSettingDto.getSelectedEventStartId()))
                throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.dialog.eventStart")), "event-start");
            for (String field : calendarSettingDto.getSelectedDisplayedFields())
                if (field.startsWith("customfield_")) {
                    if (customFieldManager.getCustomFieldObject(field) == null)
                        throw new RestFieldException("Can not find custom field with id => " + field, "fields");
                } else if (!DISPLAYED_FIELDS.contains(field))
                    throw new RestFieldException(String.format("Can not find field %s among standart fields", field), "fields");
        }
    }
}
