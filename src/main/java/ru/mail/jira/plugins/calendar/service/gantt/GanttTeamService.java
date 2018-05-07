package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.gantt.GanttUser;
import ru.mail.jira.plugins.calendar.model.gantt.GanttTeam;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTeamDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttUserDto;

import java.util.List;

@Transactional
public interface GanttTeamService {
    GanttTeam getTeam(int id) throws GetException;

    List<GanttTeamDto> getTeams(ApplicationUser currentUser, int calendarId) throws GetException;

    List<GanttTeamDto> createTeam(ApplicationUser currentUser, GanttTeamDto teamDto) throws GetException;

    List<GanttTeamDto> editTeam(ApplicationUser currentUser, GanttTeamDto teamDto) throws GetException;

    List<GanttTeamDto> deleteTeam(ApplicationUser currentUser, int id) throws GetException;

    List<UserDto> findUsers(ApplicationUser currentUser, int calendarId, String filter) throws GetException;

    List<GanttTeamDto> addUsers(ApplicationUser currentUser, int teamId, List<UserDto> selectedUsers) throws GetException;

    List<GanttTeamDto> deleteUser(ApplicationUser currentUser, int teamId, int userId) throws GetException;

    List<GanttTeamDto> updateUser(ApplicationUser currentUser, int teamId, int userId, GanttUserDto userDto) throws GetException;

    GanttUser getUser(int id) throws GetException;

    GanttUser getUser(String key);
}
