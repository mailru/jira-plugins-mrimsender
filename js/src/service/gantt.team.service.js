/* eslint-disable flowtype/require-valid-file-annotation */
// eslint-disable-next-line import/no-extraneous-dependencies
import {ajaxGet, ajaxPost, ajaxPut, ajaxDelete, getPluginBaseUrl} from '../common/ajs-helpers';


export class GanttTeamService {
    static getTeams(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/team/all?calendarId=${calendarId}`);
    }

    static createTeam(team) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/team`, team);
    }

    static editTeam(team) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/team/${team.id}`, team);
    }

    static deleteTeam(team) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/team/${team.id}`);
    }

    static findUsers(team, filter) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/team/findUsers?filter=${filter}&&calendarId=${team.calendarId}`);
    }

    static addUsers(team, users) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/team/${team.id}/users`, users);
    }

    static deleteUser(team, user) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/team/${team.id}/user/${user.id}`);
    }

    static editUser(team, user) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/team/${team.id}/user/${user.id}`, user);
    }
}
