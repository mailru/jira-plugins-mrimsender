/* eslint-disable flowtype/require-valid-file-annotation */
// eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';


export const views = {
    basic: {
        key: 'basic',
        name: i18n['ru.mail.jira.plugins.calendar.gantt.views.basic']
    },
    ganttOnly: {
        key: 'ganttOnly',
        name: i18n['ru.mail.jira.plugins.calendar.gantt.views.ganttOnly']
    },
    withResources: {
        key: 'withResources',
        name: i18n['ru.mail.jira.plugins.calendar.gantt.views.withResources']
    }
};

export const viewKeys = Object.keys(views);

export const viewItems = viewKeys.map(key => views[key]);
