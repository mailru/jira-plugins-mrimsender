// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';

import {ajaxDelete, ajaxGet, ajaxPost, ajaxPut, getPluginBaseUrl} from '../common/ajs-helpers';


export class GanttService {
    getGantt(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/${calendarId}`);
    }

    getOptimized(calendarId, params) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/${calendarId}/optimized?${$.param(params)}`);
    }

    applyPlan(calendarId, data) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/applyPlan`, data);
    }

    updateTask(calendarId, id, task) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}`, task);
    }

    estimateTask(calendarId, id, data) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}/estimate`, data);
    }

    createLink(calendarId, link) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/link`, link);
    }

    deleteLink(calendarId, id) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/${calendarId}/link/${id}`);
    }

    findSprints(query) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/sprints?query=${encodeURIComponent(query)}`);
    }

    getCalendarSprints(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/calendarSprints/${calendarId}`);
    }
}
