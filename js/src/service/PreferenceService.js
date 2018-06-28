/* eslint-disable flowtype/require-valid-file-annotation */
const prefix = 'ru.mail.jira.gantt.';

export const properties = {
    startDate: `${prefix}startDate`,
    endDate: `${prefix}endDate`,
    groupBy: `${prefix}groupBy`,
    orderBy: `${prefix}orderBy`,
    isOrderedByRank: `${prefix}isOrderedByRank`,
    order: `${prefix}order`,
    columns: `${prefix}columns`,
    withUnscheduled: `${prefix}withUnscheduled`
};

export class PreferenceService {
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

    static getOptions() {
        const result = {};

        for (const key of Object.keys(properties)) {
            const value = PreferenceService.get(properties[key]);

            if (value !== null && value !== undefined) {
                result[key] = value;
            }
        }

        return result;
    }

    static saveOptions(options) {
        for (const key of Object.keys(properties)) {
            const storeKey = properties[key];
            if (storeKey) {
                PreferenceService.put(storeKey, options[key]);
            } else {
                console.warn('unknwon store key', storeKey);
            }
        }
    }
}
