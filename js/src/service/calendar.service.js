/* eslint-disable flowtype/require-valid-file-annotation */
import {ajaxGet, getBaseUrl, getPluginBaseUrl} from '../common/ajs-helpers';


export class CalendarService {
    static getCalendar(id) {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/${id}`);
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
