package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.gantt.GanttUser;
import ru.mail.jira.plugins.calendar.model.gantt.GanttTeam;
import ru.mail.jira.plugins.calendar.model.gantt.GanttUserToGanttTeam;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttUserDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTeamDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GanttTeamServiceImpl implements GanttTeamService {
    private final ActiveObjects ao;
    private final AvatarService avatarService;
    private final CalendarService calendarService;
    private final I18nResolver i18nResolver;
    private final PermissionService permissionService;
    private final UserManager userManager;
    private final UserSearchService userSearchService;

    @Autowired
    public GanttTeamServiceImpl(
            @ComponentImport ActiveObjects ao,
            @ComponentImport AvatarService avatarService,
            @ComponentImport I18nResolver i18nResolver,
            @ComponentImport UserManager userManager,
            @ComponentImport UserSearchService userSearchService,
            CalendarService calendarService,
            PermissionService permissionService
    ) {
        this.ao = ao;
        this.avatarService = avatarService;
        this.calendarService = calendarService;
        this.i18nResolver = i18nResolver;
        this.permissionService = permissionService;
        this.userSearchService = userSearchService;
        this.userManager = userManager;
    }

    private static JSONObject formatErrorCollection(ErrorCollection errorCollection) {
        JSONObject result = new JSONObject();
        try {
            JSONObject errorsObject = new JSONObject();
            Collection<String> errorMessages = errorCollection.getErrorMessages();
            if (errorMessages != null && errorMessages.size() > 0)
                result.put("errorMessages", errorMessages);

            if (errorCollection.getErrors() != null && errorCollection.getErrors().size() > 0)
                for (Map.Entry<String, String> entry : errorCollection.getErrors().entrySet())
                    errorsObject.put(entry.getKey(), entry.getValue());
            result.put("errors", errorsObject);
        } catch (Exception ignore) {
        }

        return result;
    }

    private void validateTeam(ApplicationUser currentUser, GanttTeamDto ganttTeamDto) {
        ErrorCollection errors = new SimpleErrorCollection();
        if (currentUser == null)
            errors.addErrorMessage("User doesn't exist");
        if (StringUtils.isBlank(ganttTeamDto.getName()))
            errors.addError("field", i18nResolver.getText("issue.field.required", i18nResolver.getText("common.words.name")));
        if (ganttTeamDto.getName() != null && ganttTeamDto.getName().length() > 255)
            errors.addError("field", i18nResolver.getText("admin.customfield.type.textfield.validation.error"));

        if (errors.hasAnyErrors())
            throw new IllegalArgumentException(formatErrorCollection(errors).toString());
    }

    private void validateUser(ApplicationUser currentUser, GanttUserDto userDto) {
        ErrorCollection errors = new SimpleErrorCollection();
        if (currentUser == null)
            errors.addErrorMessage("User doesn't exist");
        try {
            Integer weeklyHours = Integer.parseInt(userDto.getWeeklyHours());
            if (weeklyHours < 0)
                errors.addError("field", "Weekly Hours must be a positive number.");
        } catch (NumberFormatException e) {
            errors.addError("field", "Weekly Hours must be a integer.");
        }
        if (errors.hasAnyErrors())
            throw new IllegalArgumentException(formatErrorCollection(errors).toString());
    }

    private final Function<ApplicationUser, UserDto> userMapping = (user) -> {
        UserDto userDto = new UserDto();
        userDto.setKey(user.getKey());
        userDto.setName(user.getName());
        userDto.setDisplayName(user.getDisplayName());
        return userDto;
    };

    private GanttUserToGanttTeam getUserToTeam(int teamId, int userId) {
        GanttUserToGanttTeam[] userToTeams = ao.find(GanttUserToGanttTeam.class, Query.select().where("USER_ID = ? AND TEAM_ID = ?", userId, teamId));
        if (userToTeams.length == 0)
            return null;
        if (userToTeams.length > 1)
            throw new IllegalArgumentException("More than one gantt user for one team");
        return userToTeams[0];
    }

    @Override
    public GanttTeam getTeam(final int id) throws GetException {
        GanttTeam team = ao.get(GanttTeam.class, id);
        if (team == null)
            throw new GetException("No Gantt Team with id = " + id);
        return team;
    }

    @Override
    public List<GanttTeamDto> getTeams(int calendarId) {
        List<GanttTeamDto> teams = new ArrayList<>();
        for (GanttTeam team : ao.find(GanttTeam.class, Query.select().where("CALENDAR_ID = ?", calendarId))) {
            GanttTeamDto teamDto = new GanttTeamDto(team);
            List<GanttUserDto> userDtos = new ArrayList<>();
            for (GanttUser user : team.getUsers()) {
                ApplicationUser jiraUser = userManager.getUserByKey(user.getKey());
                if (jiraUser != null) {
                    GanttUserDto userDto = new GanttUserDto();
                    userDto.setId(user.getID());
                    userDto.setKey(jiraUser.getKey());
                    userDto.setDisplayName(jiraUser.getDisplayName());
                    userDto.setAvatarUrl(avatarService.getAvatarURL(jiraUser, jiraUser).toString());
                    GanttUserToGanttTeam userToTeam = getUserToTeam(team.getID(), user.getID());
                    if (userToTeam != null && userToTeam.getWeeklyHours() != null)
                        userDto.setWeeklyHours(userToTeam.getWeeklyHours().toString());
                    userDtos.add(userDto);
                }
            }
            teamDto.setUsers(userDtos);
            teams.add(teamDto);
        }
        return teams;
    }

    @Override
    public List<GanttTeamDto> createTeam(ApplicationUser currentUser, GanttTeamDto teamDto) throws GetException {
        Calendar calendar = calendarService.getCalendar(teamDto.getCalendarId());
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to create team");
        validateTeam(currentUser, teamDto);
        GanttTeam team = ao.create(GanttTeam.class);
        team.setName(teamDto.getName());
        team.setCalendarId(teamDto.getCalendarId());
        team.save();
        return getTeams(teamDto.getCalendarId());
    }

    @Override
    public List<GanttTeamDto> editTeam(ApplicationUser currentUser, GanttTeamDto teamDto) throws GetException {
        Calendar calendar = calendarService.getCalendar(teamDto.getCalendarId());
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to edit team");
        validateTeam(currentUser, teamDto);
        GanttTeam team = getTeam(teamDto.getId());
        team.setName(teamDto.getName());
        team.save();
        return getTeams(teamDto.getCalendarId());
    }

    @Override
    public List<GanttTeamDto> deleteTeam(ApplicationUser currentUser, int id) throws GetException {
        GanttTeam team = getTeam(id);
        int calendarId = team.getCalendarId();
        Calendar calendar = calendarService.getCalendar(calendarId);
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to delete team");
        for (GanttUser user : team.getUsers())
            deleteUser(currentUser, id, user.getID());
        ao.delete(team);
        return getTeams(calendarId);
    }

    @Override
    public List<UserDto> findUsers(int teamId, String filter) throws GetException {
        GanttTeam team = getTeam(teamId);
        List<String> teamUsersKeys = Arrays.stream(team.getUsers())
                                   .map(GanttUser::getKey)
                                   .collect(Collectors.toList());
        return userSearchService.findUsers(filter, new UserSearchParams(true, true, false, true, null, null))
                                .stream()
                                .filter(user -> !teamUsersKeys.contains(user.getKey()))
                                .limit(10)
                                .map(userMapping)
                                .collect(Collectors.toList());
    }

    @Override
    public List<GanttTeamDto> addUsers(ApplicationUser currentUser, int teamId, List<UserDto> selectedUsers) throws GetException {
        GanttTeam team = getTeam(teamId);
        Calendar calendar = calendarService.getCalendar(team.getCalendarId());
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to edit team");
        for (UserDto selectedUser : selectedUsers) {
            GanttUser user = getUser(selectedUser.getKey());
            if (user == null) {
                user = ao.create(GanttUser.class);
                user.setKey(selectedUser.getKey());
                user.save();
            }
            GanttUserToGanttTeam userToTeam = ao.create(GanttUserToGanttTeam.class);
            userToTeam.setUser(user);
            userToTeam.setTeam(team);
            userToTeam.save();
        }
        return getTeams(team.getCalendarId());
    }

    @Override
    public List<GanttTeamDto> deleteUser(ApplicationUser currentUser, int teamId, int userId) throws GetException {
        GanttTeam team = getTeam(teamId);
        Calendar calendar = calendarService.getCalendar(team.getCalendarId());
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to edit team");
        GanttUser user = getUser(userId);
        GanttUserToGanttTeam userToTeam = getUserToTeam(teamId, userId);
        if (userToTeam != null)
            ao.delete(userToTeam);
        if (user.getTeams().length == 0)
            ao.delete(user);
        return getTeams(team.getCalendarId());
    }

    @Override
    public List<GanttTeamDto> updateUser(ApplicationUser currentUser, int teamId, int userId, GanttUserDto userDto) throws GetException {
        GanttTeam team = getTeam(teamId);
        Calendar calendar = calendarService.getCalendar(team.getCalendarId());
        if (!permissionService.hasAdminPermission(currentUser, calendar))
            throw new SecurityException("No permission to edit user");
        validateUser(currentUser, userDto);
        GanttUserToGanttTeam userToTeam = getUserToTeam(teamId, userId);
        if (userToTeam != null) {

            userToTeam.setWeeklyHours(Integer.parseInt(userDto.getWeeklyHours()));
            userToTeam.save();
        }
        return getTeams(team.getCalendarId());
    }

    @Override
    public GanttUser getUser(int id) throws GetException {
        GanttUser user = ao.get(GanttUser.class, id);
        if (user == null)
            throw new GetException("No Gantt User with id = " + id);
        return user;
    }

    @Override
    public GanttUser getUser(String key) {
        GanttUser[] users = ao.find(GanttUser.class, Query.select().where("KEY = ?", key));
        if (users.length == 0)
            return null;
        if (users.length > 1)
            throw new IllegalArgumentException(String.format("More than one gantt user is found by %s", key));
        return users[0];
    }
}
