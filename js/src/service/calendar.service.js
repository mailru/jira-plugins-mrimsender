/* eslint-disable flowtype/require-valid-file-annotation */
import {ajaxGet, ajaxPut, getBaseUrl, getPluginBaseUrl} from '../common/ajs-helpers';


export class CalendarService {
    static getCalendar(id) {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/forUser/${id}`);
    }

    static selectQuickFilter(calendarId: number, filterId: number, value: boolean) {
        return ajaxPut(`${getPluginBaseUrl()}/calendar/${calendarId}/selectQuickFilter/${filterId}`, {value})
    }

    static getEventInfo(calendarId, eventId) {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/events/${calendarId}/event/${eventId}/info`);
    }

    static getUserPreference() {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/userPreference`);
    }

    static getUserCalendars() {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/forUser`);
    }

    static getFields() {
        return ajaxGet(`${getBaseUrl()}/rest/api/latest/field`);
    }
}
