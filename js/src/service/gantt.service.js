/* eslint-disable flowtype/require-valid-file-annotation */
import queryString from 'query-string';

// eslint-disable-next-line import/no-extraneous-dependencies
import $ from 'jquery';

import {ajaxDelete, ajaxGet, ajaxPost, ajaxPut, getPluginBaseUrl, getBaseUrl} from '../common/ajs-helpers';


export class GanttService {
    static getGantt(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/${calendarId}`);
    }

    static getOptimized(calendarId, params) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/${calendarId}/optimized?${$.param(params)}`);
    }

    static applyPlan(calendarId, data) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/applyPlan`, data);
    }

    static updateTask(calendarId, id, task, queryParams={}) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}?${queryString.stringify(queryParams)}`, task);
    }

    static estimateTask(calendarId, id, data, queryParams={}) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}/estimate?${queryString.stringify(queryParams)}`, data);
    }

    static createLink(calendarId, link) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/link`, link);
    }

    static deleteLink(calendarId, id) {
        return ajaxDelete(`${getPluginBaseUrl()}/gantt/${calendarId}/link/${id}`);
    }

    static findSprints(query) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/sprints?query=${encodeURIComponent(query)}`);
    }

    static getCalendarSprints(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/calendarSprints/${calendarId}`);
    }

    static getErrors(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/errors/${calendarId}`);
    }

    static updateTaskRank(data) {
        return ajaxPut(`${getBaseUrl()}/rest/agile/1.0/issue/rank`, data);
    }
}
