// eslint-disable-next-line import/no-extraneous-dependencies
import {ajaxGet, ajaxPost, ajaxPut, ajaxDelete, getPluginBaseUrl} from '../common/ajs-helpers';


export class GanttTeamService {
    getTeams(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/team/all?calendarId=${calendarId}`);
    }

    createTeam(team) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/team`, team);
    }

    editTeam(team) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/team/${team.id}`, team);
    }

    deleteTeam(team) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/team/${team.id}`);
    }

    findUsers(team, filter) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/team/${team.id}/findUsers?filter=${filter}`);
    }

    addUsers(team, users) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/team/${team.id}/users`, users);
    }

    deleteUser(team, user) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/team/${team.id}/user/${user.id}`);
    }

    editUser(team, user) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/team/${team.id}/user/${user.id}`, user);
    }
}
