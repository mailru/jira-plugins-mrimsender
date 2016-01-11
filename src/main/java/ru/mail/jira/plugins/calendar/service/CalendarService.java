package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlClauseBuilderFactory;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Share;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.commons.RestFieldException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarService {
    private final static Logger log = LoggerFactory.getLogger(CalendarService.class);

    // TODO not thread safe
    private final static Pattern COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    private final static Pattern GROUP_PATTERN = Pattern.compile("group=(.*)");
    private final static Pattern PROJECT_ROLE_PATTERN = Pattern.compile("project=(\\d*)( role=(\\d*))?");

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
    public static List<String> DISPLAYED_FIELDS;

    static {
        DISPLAYED_FIELDS = new ArrayList<String>();
        DISPLAYED_FIELDS.add(DESCRIPTION);
        DISPLAYED_FIELDS.add(STATUS);
        DISPLAYED_FIELDS.add(ASSIGNEE);
        DISPLAYED_FIELDS.add(REPORTER);
        DISPLAYED_FIELDS.add(PRIORITY);
        DISPLAYED_FIELDS.add(CREATED);
        DISPLAYED_FIELDS.add(UPDATED);
        DISPLAYED_FIELDS.add(DUEDATE);
        DISPLAYED_FIELDS.add(COMPONENTS);
        DISPLAYED_FIELDS.add(ENVIRONMENT);
        DISPLAYED_FIELDS.add(LABELS);
        DISPLAYED_FIELDS.add(RESOLUTION);
        DISPLAYED_FIELDS.add(AFFECT);
    }

    private final UserDataService userDataService;

    private final ActiveObjects ao;
    private final CustomFieldManager customFieldManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final I18nHelper i18nHelper;
    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final SearchRequestService searchRequestService;
    private final SearchRequestManager searchRequestManager;

    public CalendarService(UserDataService userDataService,
                           ActiveObjects ao,
                           CustomFieldManager customFieldManager,
                           GlobalPermissionManager globalPermissionManager,
                           GroupManager groupManager,
                           I18nHelper i18nHelper,
                           PermissionManager permissionManager,
                           ProjectManager projectManager,
                           ProjectRoleManager projectRoleManager,
                           SearchRequestService searchRequestService,
                           SearchRequestManager searchRequestManager) {
        this.userDataService = userDataService;
        this.ao = ao;
        this.customFieldManager = customFieldManager;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.i18nHelper = i18nHelper;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.searchRequestService = searchRequestService;
        this.searchRequestManager = searchRequestManager;
    }

    private void setCalendarFields(Calendar calendar,
                                   String name,
                                   String source,
                                   String color,
                                   String eventStart,
                                   String eventEnd,
                                   String displayedFields) {
        calendar.setName(name);
        calendar.setSource(source);
        calendar.setColor(color);
        calendar.setEventStart(eventStart);
        calendar.setEventEnd(eventEnd);
        calendar.setDisplayedFields(displayedFields);
        calendar.save();
    }

    private void deleteOldShareDate(int calendarId) {
        Share[] shares = ao.find(Share.class, Query.select().where("CALENDAR_ID = ?", calendarId));
        for (Share share : shares)
            ao.delete(share);
    }

    private boolean userCanUpdateAndDeleteCalendar(ApplicationUser user, String calendarAuthorKey) {
        return isAdministrator(user) || user.getKey().equals(calendarAuthorKey);
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    private void addCalendarToShowed(Calendar calendar, ApplicationUser user) {
        UserData userData = userDataService.findUserData(user);
        if (userData != null) {
            if (StringUtils.isNotEmpty(userData.getShowedCalendars())) {
                List<String> calendarIds = new ArrayList<String>(Arrays.asList(userData.getShowedCalendars().split(";")));
                String calendarIdStr = String.valueOf(calendar.getID());
                if (!calendarIds.contains(calendarIdStr))
                    calendarIds.add(calendarIdStr);

                List<String> showedCalendars = new ArrayList<String>(calendarIds);
                Iterator<String> iterator = showedCalendars.iterator();
                while (iterator.hasNext())
                    if (ao.get(Calendar.class, Integer.parseInt(iterator.next())) == null)
                        iterator.remove();

                userData.setShowedCalendars(StringUtils.join(showedCalendars, ";"));
            } else
                userData.setShowedCalendars(String.valueOf(calendar.getID()));
        } else {
            userData = ao.create(UserData.class);
            userData.setHideWeekends(false);
            userData.setShowTime(false);
            userData.setShowedCalendars(String.valueOf(calendar.getID()));
            userData.setUserKey(user.getKey());
            userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
        }
        userData.save();
    }

    public Calendar getCalendar(final int id) {
        return ao.executeInTransaction(new TransactionCallback<Calendar>() {
            @Override
            public Calendar doInTransaction() {
                return ao.get(Calendar.class, id);
            }
        });
    }

    public Calendar[] getAllCalendars() {
        return ao.executeInTransaction(new TransactionCallback<Calendar[]>() {
            @Override
            public Calendar[] doInTransaction() {
                return ao.find(Calendar.class);
            }
        });
    }

    public Calendar createCalendar(final ApplicationUser user,
                                   final String name,
                                   final String source,
                                   final String color,
                                   final String eventStart,
                                   final String eventEnd,
                                   final String displayedFields,
                                   final String shares) {
        return createCalendar(user, name, source, color, eventStart, eventEnd, displayedFields, shares, true, false);
    }

    public Calendar createCalendar(final ApplicationUser user,
                                   final String name,
                                   final String source,
                                   final String color,
                                   final String eventStart,
                                   final String eventEnd,
                                   final String displayedFields,
                                   final String shares,
                                   final boolean visible,
                                   final boolean fromMigration) {
        return ao.executeInTransaction(new TransactionCallback<Calendar>() {
            @Override
            public Calendar doInTransaction() {
                validateCalendar(user, name, source, color, eventStart, displayedFields, shares, true, fromMigration);
                final List<LocalShare> calendarShares = getLocalShares(user, shares);
                Calendar calendar = ao.create(Calendar.class);
                calendar.setAuthorKey(user.getKey());
                setCalendarFields(calendar, name, source, color, eventStart, eventEnd, displayedFields);

                createNewShares(calendar, calendarShares);
                if (visible)
                    addCalendarToShowed(calendar, user);
                return calendar;
            }
        });
    }

    public Calendar updateCalendar(final ApplicationUser user,
                                   final int calendarId,
                                   final String name,
                                   final String source,
                                   final String color,
                                   final String eventStart,
                                   final String eventEnd,
                                   final String displayedFields,
                                   final String shares) {
        return ao.executeInTransaction(new TransactionCallback<Calendar>() {
            @Override
            public Calendar doInTransaction() {
                Calendar calendar = getCalendar(calendarId);

                if (!userCanUpdateAndDeleteCalendar(user, calendar.getAuthorKey()))
                    throw new SecurityException("No permission to edit calendar");

                validateCalendar(user, name, source, color, eventStart, displayedFields, shares, false, false);
                List<LocalShare> calendarShares = getLocalShares(user, shares);
                setCalendarFields(calendar, name, source, color, eventStart, eventEnd, displayedFields);
                deleteOldShareDate(calendarId);
                createNewShares(calendar, calendarShares);
                return calendar;
            }
        });
    }

    public void deleteCalendar(final ApplicationUser user, final int calendarId) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                Calendar calendar = getCalendar(calendarId);

                if (!userCanUpdateAndDeleteCalendar(user, calendar.getAuthorKey()))
                    throw new SecurityException("No permission to edit calendar");

                for (Share share : calendar.getShares())
                    ao.delete(share);

                try {
                    for (UserData userData : ao.find(UserData.class)) {
                        boolean userDataChanged = false;
                        String showedCalendarsStr = userData.getShowedCalendars();
                        if (StringUtils.isEmpty(showedCalendarsStr) || !showedCalendarsStr.contains(String.valueOf(calendarId)))
                            continue;

                        List<Integer> showedCalendars = new ArrayList<Integer>();
                        for (String showedCalendarIdStr : showedCalendarsStr.split(";"))
                            try {
                                int showedCalendarId = Integer.parseInt(showedCalendarIdStr);
                                if (showedCalendarId == calendarId || ao.get(Calendar.class, showedCalendarId) == null)
                                    userDataChanged = true;
                                else
                                    showedCalendars.add(showedCalendarId);
                            } catch (NumberFormatException e) {
                                userDataChanged = true;
                            }

                        Set<Integer> favoriteCalendars = userDataService.getFavoriteCalendars(userData);
                        if (favoriteCalendars.remove(calendarId))
                            userDataChanged = true;

                        if (userDataChanged) {
                            userData.setShowedCalendars(StringUtils.join(showedCalendars, ";"));
                            userData.setFavoriteCalendars(StringUtils.join(favoriteCalendars, ";"));
                            userData.save();
                        }
                    }
                } catch (Exception e) {
                    log.error("Error while trying to delete all calendar id from user data", e);
                }

                ao.delete(calendar);
                return null;
            }
        });
    }

    private void validateCalendar(ApplicationUser user,
                                  String name,
                                  String source,
                                  String color,
                                  String eventStart,
                                  String displayedFields,
                                  String shares,
                                  boolean isCreate,
                                  boolean fromMigrate) {
        if (user == null)
            throw new IllegalArgumentException("User doesn't exist");
        if (StringUtils.isBlank(name))
            throw new RestFieldException(i18nHelper.getText("issue.field.required", i18nHelper.getText("common.words.name")), "name");
        if (StringUtils.isBlank(source))
            throw new RestFieldException(i18nHelper.getText("issue.field.required", i18nHelper.getText("ru.mail.jira.plugins.calendar.dialog.source")), "source");
        if (StringUtils.isBlank(color))
            throw new RestFieldException(i18nHelper.getText("issue.field.required", i18nHelper.getText("admin.common.words.color")), "color");
        if (StringUtils.isBlank(eventStart))
            throw new RestFieldException(i18nHelper.getText("issue.field.required", i18nHelper.getText("ru.mail.jira.plugins.calendar.dialog.eventStart")), "event-start");

        if (!COLOR_PATTERN.matcher(color).matches())
            throw new IllegalArgumentException("Bad color => " + color);

        if (!source.startsWith("project_") && !source.startsWith("filter_"))
            throw new IllegalArgumentException("Bad source => " + source);

        boolean isUserAdmin = isAdministrator(user);

        try {
            if (source.startsWith("project_")) {
                long projectId = Long.parseLong(source.substring("project_".length()));
                if (isCreate) {
                    Project project = projectManager.getProjectObj(projectId);
                    if (project == null)
                        throw new RestFieldException("Can not find project with id => " + projectId, "source");

                    if (!permissionManager.hasPermission(Permissions.BROWSE, project, user, false))
                        throw new RestFieldException("No Permission to browse project " + project.getName(), "source");
                }
            } else if (source.startsWith("filter_")) {
                long filterId = Long.parseLong(source.substring("filter_".length()));
                if (isCreate) {
                    if (searchRequestManager.getSearchRequestById(filterId) == null)
                        throw new RestFieldException("Can not find filter with id " + filterId, "source");

                    JiraServiceContext serviceContext = new JiraServiceContextImpl(user);
                    searchRequestService.getFilter(serviceContext, filterId);
                    if (serviceContext.getErrorCollection().hasAnyErrors())
                        throw new RestFieldException(serviceContext.getErrorCollection().getErrorMessages().toString(), "source");
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad source => " + source);
        }

        if (StringUtils.isNotBlank(displayedFields)) {
            for (String field : displayedFields.split(",")) {
                if (field.startsWith("customfield_")) {
                    if (customFieldManager.getCustomFieldObject(field) == null)
                        throw new RestFieldException("Can not find custom field with id => " + field, "fields");
                } else if (!DISPLAYED_FIELDS.contains(field)) {
                    throw new RestFieldException(String.format("Can not find field %s among standart fields", field), "fields");
                }
            }
        }

        if (StringUtils.isNotBlank(shares)) {
            for (String shareExpr : shares.split(";")) {
                LocalShare groupFromShare = getGroupFromExpr(shareExpr);
                LocalShare projectRoleFromShare = getProjectRoleFromExpr(user, shareExpr);

                if (groupFromShare != null) {
                    if (!groupManager.groupExists(groupFromShare.groupName))
                        throw new RestFieldException(i18nHelper.getText("admin.viewgroup.group.does.not.exist"), "group_" + groupFromShare.groupName);
                    if (!fromMigrate && !isUserAdmin && !groupManager.isUserInGroup(user.getDirectoryUser().getName(), groupFromShare.groupName))
                        throw new RestFieldException(i18nHelper.getText("common.sharing.exception.not.in.group", groupFromShare.groupName), "group_" + groupFromShare.groupName);
                } else if (projectRoleFromShare != null) {
                    Project project = projectManager.getProjectObj(projectRoleFromShare.projectId);
                    if (project == null)
                        throw new RestFieldException(i18nHelper.getText("common.sharing.exception.project.does.not.exist"), "project_" + projectRoleFromShare.projectId);

                    if (!fromMigrate) {
                        if (!isUserAdmin && !permissionManager.hasPermission(Permissions.BROWSE, project, user, false))
                            throw new RestFieldException(i18nHelper.getText("common.sharing.exception.no.permission.project", project.getName()),
                                                         "project_" + projectRoleFromShare.projectId);

                        if (projectRoleFromShare.roleId != null) {
                            ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleFromShare.roleId);
                            if (projectRole == null)
                                throw new RestFieldException(i18nHelper.getText("admin.errors.specified.role.does.not.exist"), "project_role_" + projectRoleFromShare.roleId);

                            if (!isUserAdmin && !projectRoleManager.isUserInProjectRole(user, projectRole, project))
                                throw new RestFieldException(i18nHelper.getText("common.sharing.exception.no.permission.role", project.getName(), projectRole.getName()), "project_role_" + projectRoleFromShare.roleId);
                        }
                    }
                } else
                    throw new IllegalArgumentException("Bad shares value => " + shares);
            }
        }
    }

    public boolean invertCalendarVisibility(final int calendarId, final ApplicationUser user) {
        return ao.executeInTransaction(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() {
                Calendar calendar = getCalendar(calendarId);
                UserData userData = userDataService.findUserData(user);
                if (userData == null) {
                    userData = ao.create(UserData.class);
                    userData.setHideWeekends(false);
                    userData.setShowTime(false);
                    userData.setShowedCalendars(String.valueOf(calendar.getID()));
                    userData.setUserKey(user.getKey());
                    userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                    userData.save();
                    return true;
                }

                if (StringUtils.isEmpty(userData.getShowedCalendars())) {
                    userData.setShowedCalendars(String.valueOf(calendar.getID()));
                    userData.save();
                    return true;
                }

                List<String> calendarIds = new ArrayList<String>(Arrays.asList(userData.getShowedCalendars().split(";")));
                String calendarIdStr = String.valueOf(calendarId);
                if (calendarIds.contains(calendarIdStr)) {
                    calendarIds.remove(calendarIdStr);
                    userData.setShowedCalendars(StringUtils.join(calendarIds, ";"));
                    userData.save();
                    return false;
                }

                calendarIds.add(calendarIdStr);
                userData.setShowedCalendars(StringUtils.join(calendarIds, ";"));
                userData.save();
                return true;
            }
        });
    }

    public void deleteShare(final Share share) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                ao.delete(share);
                return null;
            }
        });
    }

    class LocalShare {
        String groupName;
        Long projectId;
        Long roleId;

        LocalShare(String groupName) {
            this.groupName = groupName;
        }

        LocalShare(Long projectId, Long roleId) {
            this.projectId = projectId;
            this.roleId = roleId;
        }
    }

    private void createNewShares(Calendar calendar, List<LocalShare> calendarShares) {
        for (LocalShare localShare : calendarShares) {
            Share share = ao.create(Share.class);
            if (localShare.groupName != null)
                share.setGroup(localShare.groupName);
            else {
                share.setProject(localShare.projectId);
                if (localShare.roleId != null)
                    share.setRole(localShare.roleId);
            }
            share.setCalendar(calendar);
            share.save();
        }
    }

    private List<LocalShare> getLocalShares(ApplicationUser user, String shares) {
        final List<LocalShare> calendarShares = new ArrayList<LocalShare>();
        if (StringUtils.isNotBlank(shares)) {
            Set<String> uniqueShares = new HashSet<String>();
            for (String shareExpr : shares.split(";")) {
                if (!uniqueShares.add(shareExpr))
                    continue;

                LocalShare localShare;
                if ((localShare = getGroupFromExpr(shareExpr)) != null)
                    calendarShares.add(localShare);
                else if ((localShare = getProjectRoleFromExpr(user, shareExpr)) != null)
                    calendarShares.add(localShare);
            }
        }
        return calendarShares;
    }

    @Nullable
    private LocalShare getGroupFromExpr(String expr) {
        Matcher matcher = GROUP_PATTERN.matcher(expr);
        if (matcher.matches()) {
            String groupName = matcher.group(1);
            if (!groupManager.groupExists(groupName))
                throw new IllegalArgumentException(String.format("Group with name => %s doesn't exist", groupName));
            return new LocalShare(groupName);
        }
        return null;
    }

    @Nullable
    private LocalShare getProjectRoleFromExpr(ApplicationUser user, String expr) {
        Matcher matcher = PROJECT_ROLE_PATTERN.matcher(expr);
        if (matcher.matches()) {
            Long projectId = Long.parseLong(matcher.group(1));

            Project project = projectManager.getProjectObj(projectId);
            if (project == null)
                throw new IllegalArgumentException("Bad project id in shares => " + projectId);
            if (!permissionManager.hasPermission(Permissions.BROWSE, project, user, false))
                throw new IllegalArgumentException("No permission to view project in shares => " + project.getName());

            String roleIdStr = matcher.group(3);
            if (StringUtils.isNotEmpty(roleIdStr)) {
                long roleId = Long.parseLong(roleIdStr);
                ProjectRole projectRole = projectRoleManager.getProjectRole(roleId);
                if (projectRole == null)
                    throw new IllegalArgumentException("Can not found project role with id => " + roleIdStr);
                return new LocalShare(projectId, roleId);
            }
            return new LocalShare(projectId, null);
        }
        return null;
    }
}
