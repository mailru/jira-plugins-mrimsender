/* eslint-disable flowtype/require-valid-file-annotation */
import { preferenceService, storeService } from './services';

const OPTIONS_KEY_PREFIX = 'ru.mail.jira.gantt.options';
const properties = [
    'startDate',
    'endDate',
    'groupBy',
    'orderBy',
    'isOrderedByRank',
    'order',
    'columns',
    'withUnscheduled',
    'hideProgress',
];

export class PreferenceService {
    static currentOptions;
    static currentCalendarId;

    static getPropertyPrefix() {
        return 'ru.mail.jira.gantt.';
    }

    static get(key) {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    }

    static put(key, value) {
        if (value !== null && value !== undefined) {
            localStorage.setItem(key, JSON.stringify(value));
        } else {
            PreferenceService.remove(key);
        }
    }

    static remove(key) {
        localStorage.removeItem(key);
    }

    static getOptions(calendarId) {
        return PreferenceService.get(`${OPTIONS_KEY_PREFIX}.${calendarId || PreferenceService.get('ru.mail.jira.gantt.lastGantt')}`);
    }

    static saveOptions(options) {
        const calendarId = PreferenceService.get('ru.mail.jira.gantt.lastGantt');
        if (!calendarId)
            return;
        const updateOptions = PreferenceService.getOptions(calendarId) || {};
        for (const key of Object.keys(options)) {
            if (properties.includes(key))
                updateOptions[key] = options[key];
        }

        PreferenceService.put(`${OPTIONS_KEY_PREFIX}.${calendarId}`, updateOptions);
    }

    static handleStateChange() {
        const previousValue = PreferenceService.currentOptions;
        PreferenceService.currentOptions = storeService.store.getState().options;
        if (PreferenceService.currentOptions && (!previousValue || properties.some((key) => previousValue[key] !== PreferenceService.currentOptions[key]))) {
            PreferenceService.saveOptions(PreferenceService.currentOptions);
        }

        const previousId = PreferenceService.currentCalendarId;
        PreferenceService.currentCalendarId = storeService.store.getState().calendar.id;
        if(!previousId || previousId !== PreferenceService.currentCalendarId) {
            preferenceService.put('ru.mail.jira.gantt.lastGantt', PreferenceService.currentCalendarId);
        }
    }
}
