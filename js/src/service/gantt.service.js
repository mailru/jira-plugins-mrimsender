import queryString from 'query-string';

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

    updateTask(calendarId, id, task, queryParams={}) {
        return ajaxPut(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}?${queryString.stringify(queryParams)}`, task);
    }

    estimateTask(calendarId, id, data, queryParams={}) {
        return ajaxPost(`${getPluginBaseUrl()}/gantt/${calendarId}/task/${id}/estimate?${queryString.stringify(queryParams)}`, data);
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

    getErrors(calendarId) {
        return ajaxGet(`${getPluginBaseUrl()}/gantt/errors/${calendarId}`);
    }
}
