const prefix = 'ru.mail.jira.gantt.';

export const properties = {
    startDate: `${prefix}startDate`,
    endDate: `${prefix}endDate`,
    groupBy: `${prefix}groupBy`,
    orderBy: `${prefix}orderBy`,
    order: `${prefix}order`,
    columns: `${prefix}columns`
};

export class PreferenceService {
    get(key) {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : null;
    }

    put(key, value) {
        if (value) {
            localStorage.setItem(key, JSON.stringify(value));
        } else {
            this.remove(key);
        }
    }

    remove(key) {
        localStorage.removeItem(key);
    }

    getOptions() {
        const result = {};

        for (const key of Object.keys(properties)) {
            const value = this.get(properties[key]);

            if (value) {
                result[key] = value;
            }
        }

        return result;
    }

    saveOptions(options) {
        for (const key of Object.keys(properties)) {
            const storeKey = properties[key];
            if (storeKey) {
                this.put(storeKey, options[key]);
            } else {
                console.warn('unknwon store key', storeKey);
            }
        }
    }
}
