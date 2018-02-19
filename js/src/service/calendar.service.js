import {ajaxGet, getPluginBaseUrl} from '../common/ajs-helpers';


export class CalendarService {
    getCalendar(id) {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/${id}`);
    }

    getEventInfo(calendarId, eventId) {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/events/${calendarId}/event/${eventId}/info`);
    }

    getUserPreference() {
        return ajaxGet(`${getPluginBaseUrl()}/calendar/userPreference`);
    }
}
