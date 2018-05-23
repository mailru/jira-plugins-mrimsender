//@flow
//eslint-disable-next-line import/no-extraneous-dependencies
import i18n from 'i18n';

import {escapeHtml, getBaseUrl, getContextPath} from '../../common/ajs-helpers';
import type {GanttTask} from './types';


function getIconSrc(src) {
    if (src.startsWith('http')) {
        return src;
    }
    return getContextPath() + src;
}

export function buildJiraFieldColumn({key, name, colParams}, resizable=true) {
    return {
        ...colParams,
        name: key,
        label: name,
        align: 'left',
        isJiraField: true,
        resize: resizable,
        template: (item) => {
            if (item.fields) {
                return item.fields[key] || '';
            }
            return '';
        }
    };
}

export const ganttColumns = {
    id: {
        name: 'id',
        resize: true,
        tree: true,
        width: '110px',
        label: 'Код',
        align: 'left',
        template: (item: GanttTask) => {
            if (item.type === 'group' || item.type === 'sprint') {
                return '';
            }
            const id = escapeHtml(item.entityId);
            return `<a href="${getBaseUrl()}/browse/${id}" ${item.resolved ? 'style="text-decoration: line-through;"' : ''}>${id}</a>`;
        }
    },
    name: {
        name: 'summary',
        resize: true,
        label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.name'],
        width: '*',
        align: 'left',
        template: (item: GanttTask) => {
            if (!item.icon_src) {
                return escapeHtml(item.summary);
            }

            return `<img
                class="calendar-event-issue-type"
                alt="" height="16" width="16" style="margin-right: 5px;"
                src="${item.type === 'group' ? item.icon_src : getIconSrc(item.icon_src)}"/> ${escapeHtml(item.summary)}`
        }
    },
    progress: {
        name: 'progress',
        label: i18n['ru.mail.jira.plugins.calendar.gantt.columns.progress'],
        width: '80px',
        template: (item: GanttTask) => {
            if (item.type === 'issue') {
                const progress = item.progress || 0;
                const overdue = progress > 1;

                return (
                    `<div class="progressBar">
                        <div class="progressIndicator ${overdue ? 'overdue' : ''}" style="width: ${(overdue ? 1 : progress) * (80 - 12)}px"></div>
                    </div>`
                );
            }
            return '';
        },
    }
};

export const emptyColumn = {
    name: '___empty',
    resize: false,
    label: '',
    width: '12px',
    template: () => ''
};

export const defaultColumns = Object
    .keys(ganttColumns)
    .map(key => {
        return {
            key
        };
    });
